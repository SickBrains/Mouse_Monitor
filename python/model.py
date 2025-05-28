import os
import json
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import roc_auc_score, confusion_matrix, classification_report
from sklearn.utils import resample

# CONFIG
USERS = ['user7', 'user9', 'user12', 'user15', 'user16', 'user20', 'user21', 'user23', 'user29', 'user35']
FEATURES_FOLDER_TEMPLATE = r"C:\Users\ADMIN\Documents\GitHub\Mouseozp\python\balabit\training_files\{}\features_enhanced"
TEST_FOLDER_TEMPLATE = r"C:\Users\ADMIN\Documents\GitHub\Mouseozp\python\balabit\test_files\{}\features_enhanced"
PUBLIC_LABELS_CSV = r"C:\Users\ADMIN\Documents\GitHub\Mouseozp\python\balabit\public_labels.csv"

NUM_ACTIONS_FOR_SET_EVAL = 20  # Number of actions per set for set-based evaluation



def load_features(filepath):
    with open(filepath, 'r') as f:
        data = json.load(f)
    features = []
    for seg in data['segments']:
        # Extract numerical features only (skip points and type)
        feat_vector = []
        for k, v in seg.items():
            if k in ('points', 'type'):
                continue
            if v is None:
                feat_vector.append(0.0)
            else:
                feat_vector.append(float(v))
        features.append(feat_vector)
    return np.array(features)

def build_user_datasets(users):
    """
    Load and prepare training data for each user:
    For each user, balance genuine vs impostor actions by downsampling impostors.
    """
    user_train_data = {}
    for user in users:
        train_folder = FEATURES_FOLDER_TEMPLATE.format(user)
        user_files = [f for f in os.listdir(train_folder) if f.endswith('_segments_with_features.json')]
        
        
        # Load all genuine actions
        genuine_feats = []
        for file in user_files:
            feats = load_features(os.path.join(train_folder, file))
            genuine_feats.append(feats)
        genuine_feats = np.vstack(genuine_feats)
        
        # Load impostor actions from all other users
        impostor_feats = []
        for other_user in users:
            if other_user == user:
                continue
            other_folder = FEATURES_FOLDER_TEMPLATE.format(other_user)
            other_files = [f for f in os.listdir(other_folder) if f.endswith('_segments_with_features.json')]
            for file in other_files:
                feats = load_features(os.path.join(other_folder, file))
                impostor_feats.append(feats)
        impostor_feats = np.vstack(impostor_feats)
        
        # Downsample impostor to balance classes
        n_genuine = genuine_feats.shape[0]
        impostor_feats_bal = resample(impostor_feats, replace=False, n_samples=n_genuine, random_state=42)
        
        X = np.vstack([genuine_feats, impostor_feats_bal])
        y = np.hstack([np.ones(n_genuine), np.zeros(n_genuine)])
        
        user_train_data[user] = (X, y)
    return user_train_data

def train_classifiers(user_train_data):
    classifiers = {}
    for user, (X, y) in user_train_data.items():
        clf = RandomForestClassifier(n_estimators=100, random_state=42)
        clf.fit(X, y)
        classifiers[user] = clf
    return classifiers

def score_session(session_filename, test_folder, clf):
    filepath = os.path.join(test_folder, session_filename + '_segments_with_features.json')
    if not os.path.exists(filepath):
        print(f"[DEBUG] Feature file not found: {filepath}")
        return None
    features = load_features(filepath)
    if features.size == 0:
        print(f"[DEBUG] Feature file empty: {filepath}")
        return None
    probs = clf.predict_proba(features)[:, 1]
    return probs

def action_based_evaluation(classifiers, users, labels_df):
    all_true = []
    all_scores = []
    skipped = 0
    total = 0
    for _, row in labels_df.iterrows():
        user = row['user']
        if user not in users:
            print(f"[DEBUG] User {user} not in USERS list, skipping")
            continue
        clf = classifiers[user]
        test_folder = TEST_FOLDER_TEMPLATE.format(user)
        session_name = row['filename']
        print(f"[DEBUG] Scoring session: {session_name} for user: {user}")
        probs = score_session(session_name, test_folder, clf)
        total += 1
        if probs is None or len(probs) == 0:
            print(f"[DEBUG] No probabilities returned for session: {session_name}")
            skipped += 1
            continue
        all_scores.extend(probs)
        all_true.extend([row['is_illegal']] * len(probs))
    print(f"Action evaluation: processed {total} sessions, skipped {skipped} due to missing or empty features.")
    if len(all_true) == 0:
        raise ValueError("No valid test samples found for action-based evaluation.")
    all_true = np.array(all_true)
    all_scores = np.array(all_scores)
    auc = roc_auc_score(all_true, all_scores)
    pred = (all_scores >= 0.5).astype(int)
    cm = confusion_matrix(all_true, pred)
    cr = classification_report(all_true, pred, digits=4)
    acc = np.mean(pred == all_true)
    return auc, cm, cr, acc

def set_of_actions_evaluation(classifiers, users, labels_df, num_actions=NUM_ACTIONS_FOR_SET_EVAL):
    all_true = []
    all_scores = []
    skipped = 0
    total = 0
    for _, row in labels_df.iterrows():
        user = row['user']
        if user not in users:
            continue
        clf = classifiers[user]
        test_folder = TEST_FOLDER_TEMPLATE.format(user)
        probs = score_session(row['filename'], test_folder, clf)
        total += 1
        if probs is None or len(probs) < num_actions:
            skipped += 1
            continue
        n_sets = len(probs) // num_actions
        for i in range(n_sets):
            set_probs = probs[i*num_actions:(i+1)*num_actions]
            avg_prob = np.mean(set_probs)
            all_scores.append(avg_prob)
            all_true.append(row['is_illegal'])
    print(f"Set evaluation: processed {total} sessions, skipped {skipped} due to missing/insufficient features.")
    if len(all_true) == 0:
        raise ValueError("No valid test samples found for set-of-actions evaluation.")
    all_true = np.array(all_true)
    all_scores = np.array(all_scores)
    auc = roc_auc_score(all_true, all_scores)
    pred = (all_scores >= 0.5).astype(int)
    cm = confusion_matrix(all_true, pred)
    cr = classification_report(all_true, pred, digits=4)
    acc = np.mean(pred == all_true)
    return auc, cm, cr, acc



def main():
    print("Loading public labels...")
    labels_df = pd.read_csv(PUBLIC_LABELS_CSV)
    print("Sample labels data:")
    print(labels_df.head())

    # Ensure user column exists or add it by parsing filename prefix if needed
    # Assuming filenames like user7_session_XXX
    labels_df = pd.read_csv(PUBLIC_LABELS_CSV)

    def find_user_for_session(filename):
        for user in USERS:
            test_folder = TEST_FOLDER_TEMPLATE.format(user)
            filepath = os.path.join(test_folder, filename + '_segments_with_features.json')
            if os.path.exists(filepath):
                return user
        return None

    labels_df['user'] = labels_df['filename'].apply(find_user_for_session)
    labels_df = labels_df.dropna(subset=['user'])

    print("User mapping after guess:")
    print(labels_df[['filename','user']].head())


        

    print("Preparing training data for all users...")
    user_train_data = build_user_datasets(USERS)

    print("Training classifiers...")
    classifiers = train_classifiers(user_train_data)

    print("\nEvaluating Action-based scenario:")
    auc_a, cm_a, cr_a, acc_a = action_based_evaluation(classifiers, USERS, labels_df)
    print(f"AUC: {auc_a:.4f}")
    print("Confusion Matrix:\n", cm_a)
    print("Classification Report:\n", cr_a)
    print(f"Accuracy: {acc_a:.4f}")

    print("\nEvaluating Set-of-Actions scenario:")
    auc_s, cm_s, cr_s, acc_s = set_of_actions_evaluation(classifiers, USERS, labels_df, NUM_ACTIONS_FOR_SET_EVAL)
    print(f"AUC: {auc_s:.4f}")
    print("Confusion Matrix:\n", cm_s)
    print("Classification Report:\n", cr_s)
    print(f"Accuracy: {acc_s:.4f}")

if __name__ == '__main__':
    main()
