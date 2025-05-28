import os
import json
import re
import math
import numpy as np
import pandas as pd

# ----------- CONFIGURATION --------------
USERS = ['user7', 'user9', 'user12', 'user15', 'user16', 'user20', 'user21', 'user23', 'user29', 'user35']

BASE_TRAIN_PATH = r"C:\Users\ADMIN\Documents\GitHub\Mouseozp\python\balabit\training_files"
BASE_TEST_PATH = r"C:\Users\ADMIN\Documents\GitHub\Mouseozp\python\balabit\test_files"

GLOBAL_MIN_ACTION_LENGTH = 4
CURV_THRESHOLD = 0.0005  # curvature threshold

# ----------- SEGMENTATION FUNCTIONS ------------

def get_action_type(button, state):
    if button == 'Left' and state == 'Pressed':
        return 'PointClick'
    if (button == 'Left' and state == 'Released') or (button == 'NoButton' and state == 'Drag'):
        return 'DragDrop'
    if button == 'NoButton' and state == 'Move':
        return 'MouseMove'
    return 'Unknown'

def segment_and_save(df_cleaned, file_path, segment_output_folder):
    segments = []
    segment_points = []
    segment_type = None
    segment_start = 0
    mousemove_buffer = []

    for i, row in df_cleaned.iterrows():
        current_time = float(row['client timestamp'])
        button = row['button']
        state = row['state']

        current_type = get_action_type(button, state)

        if current_type == 'MouseMove':
            mousemove_buffer.append({
                'x': int(row['x']),
                'y': int(row['y']),
                't': current_time,
                'button': button,
                'state': state
            })

        if current_type == 'PointClick' and segment_type != 'PointClick':
            if segment_points and len(segment_points) >= GLOBAL_MIN_ACTION_LENGTH:
                segments.append({
                    'type': segment_type,
                    'start_index': segment_start,
                    'end_index': i - 1,
                    'points': [segment_points]
                })
            segment_points = mousemove_buffer.copy()
            segment_type = 'PointClick'
            segment_start = i - len(mousemove_buffer)
            if segment_start < 0:
                segment_start = 0
            mousemove_buffer.clear()

        elif current_type != segment_type:
            if segment_points and len(segment_points) >= GLOBAL_MIN_ACTION_LENGTH:
                segments.append({
                    'type': segment_type,
                    'start_index': segment_start,
                    'end_index': i - 1,
                    'points': [segment_points]
                })
            segment_points = []
            segment_type = current_type
            segment_start = i
            if current_type != 'MouseMove':
                mousemove_buffer.clear()

        if not (current_type == 'PointClick' and segment_type == 'PointClick' and len(segment_points) > 0 and i - segment_start < len(segment_points)):
            segment_points.append({
                'x': int(row['x']),
                'y': int(row['y']),
                't': current_time,
                'button': button,
                'state': state
            })

    if segment_points and len(segment_points) >= GLOBAL_MIN_ACTION_LENGTH:
        segments.append({
            'type': segment_type,
            'start_index': segment_start,
            'end_index': df_cleaned.index[-1],
            'points': [segment_points]
        })

    output = {'segments': segments}

    filename = os.path.basename(file_path)
    output_path = os.path.join(segment_output_folder, f"{filename}_segments.json")

    with open(output_path, 'w') as f:
        json.dump(output, f, separators=(',', ':'))

    print(f"Segmented file saved to {output_path}")

# ----------- FEATURE EXTRACTION FUNCTIONS ------------

def compute_vx_stats(points):
    x = [float(p['x']) for p in points]
    t = [float(p['t']) for p in points]
    vx = []
    for i in range(1, len(points)):
        dt = t[i] - t[i-1] or 0.01
        vx.append((x[i] - x[i-1]) / dt)
    if not vx:
        return {'mean_vx': None, 'std_vx': None, 'min_vx': None, 'max_vx': None}
    return {'mean_vx': np.mean(vx), 'std_vx': np.std(vx, ddof=1), 'min_vx': np.min(vx), 'max_vx': np.max(vx)}

def compute_vy_stats(points):
    y = [float(p['y']) for p in points]
    t = [float(p['t']) for p in points]
    vy = []
    for i in range(1, len(points)):
        dt = t[i] - t[i-1] or 0.01
        vy.append((y[i] - y[i-1]) / dt)
    if not vy:
        return {'mean_vy': None, 'std_vy': None, 'min_vy': None, 'max_vy': None}
    return {'mean_vy': np.mean(vy), 'std_vy': np.std(vy, ddof=1), 'min_vy': np.min(vy), 'max_vy': np.max(vy)}

def compute_v_stats(points):
    x = [float(p['x']) for p in points]
    y = [float(p['y']) for p in points]
    t = [float(p['t']) for p in points]
    v = []
    for i in range(1, len(points)):
        dt = t[i] - t[i-1] or 0.01
        dx = x[i] - x[i-1]
        dy = y[i] - y[i-1]
        v.append(math.sqrt(dx*dx + dy*dy) / dt)
    if not v:
        return {'mean_v': None, 'std_v': None, 'min_v': None, 'max_v': None}
    return {'mean_v': np.mean(v), 'std_v': np.std(v, ddof=1), 'min_v': np.min(v), 'max_v': np.max(v)}

def compute_a_stats(points):
    x = [float(p['x']) for p in points]
    y = [float(p['y']) for p in points]
    t = [float(p['t']) for p in points]
    v = []
    for i in range(1, len(points)):
        dt = t[i] - t[i-1] or 0.01
        dx = x[i] - x[i-1]
        dy = y[i] - y[i-1]
        v.append(math.sqrt(dx*dx + dy*dy) / dt)
    if len(v) < 2:
        return {'mean_a': None, 'std_a': None, 'min_a': None, 'max_a': None}
    a = []
    for i in range(1, len(v)):
        dt = t[i+1] - t[i] or 0.01
        dv = v[i] - v[i-1]
        a.append(dv / dt)
    if not a:
        return {'mean_a': None, 'std_a': None, 'min_a': None, 'max_a': None}
    return {'mean_a': np.mean(a), 'std_a': np.std(a, ddof=1), 'min_a': np.min(a), 'max_a': np.max(a)}

def compute_j_stats(points):
    x = [float(p['x']) for p in points]
    y = [float(p['y']) for p in points]
    t = [float(p['t']) for p in points]
    v = []
    for i in range(1, len(points)):
        dt = t[i] - t[i-1] or 0.01
        dx = x[i] - x[i-1]
        dy = y[i] - y[i-1]
        v.append(math.sqrt(dx*dx + dy*dy) / dt)
    if len(v) < 2:
        return {'mean_j': None, 'std_j': None, 'min_j': None, 'max_j': None}
    a = []
    for i in range(1, len(v)):
        dt = t[i+1] - t[i] or 0.01
        dv = v[i] - v[i-1]
        a.append(dv / dt)
    if len(a) < 2:
        return {'mean_j': None, 'std_j': None, 'min_j': None, 'max_j': None}
    j = []
    for i in range(1, len(a)):
        dt = t[i+2] - t[i+1] or 0.01
        da = a[i] - a[i-1]
        j.append(da / dt)
    if not j:
        return {'mean_j': None, 'std_j': None, 'min_j': None, 'max_j': None}
    std_j = np.std(j, ddof=1) if len(j) > 1 else 0
    return {'mean_j': np.mean(j), 'std_j': std_j, 'min_j': np.min(j), 'max_j': np.max(j)}

def compute_omega_stats(points):
    t = [float(p['t']) for p in points]
    x = [float(p['x']) for p in points]
    y = [float(p['y']) for p in points]
    angles = [0]
    for i in range(1, len(points)):
        dx = x[i] - x[i-1]
        dy = y[i] - y[i-1]
        angles.append(math.atan2(dy, dx))
    omega = []
    for i in range(1, len(angles)):
        dtheta = angles[i] - angles[i-1]
        dt = t[i] - t[i-1] or 0.01
        omega.append(dtheta / dt)
    if not omega:
        return {'mean_omega': None, 'std_omega': None, 'min_omega': None, 'max_omega': None}
    return {'mean_omega': np.mean(omega), 'std_omega': np.std(omega, ddof=1), 'min_omega': np.min(omega), 'max_omega': np.max(omega)}

def compute_curvature_stats(points):
    t = [float(p['t']) for p in points]
    x = [float(p['x']) for p in points]
    y = [float(p['y']) for p in points]
    angles = [0]
    path = [0]
    for i in range(1, len(points)):
        dx = x[i] - x[i-1]
        dy = y[i] - y[i-1]
        distance = math.sqrt(dx*dx + dy*dy)
        path.append(path[-1] + distance)
        angles.append(math.atan2(dy, dx))
    curvature = []
    for i in range(1, len(path)):
        dp = path[i] - path[i-1]
        if dp == 0:
            continue
        dangle = angles[i] - angles[i-1]
        curvature.append(dangle / dp)
    if not curvature:
        return {'mean_curv': None, 'std_curv': None, 'min_curv': None, 'max_curv': None}
    return {'mean_curv': np.mean(curvature), 'std_curv': np.std(curvature, ddof=1), 'min_curv': np.min(curvature), 'max_curv': np.max(curvature)}

def compute_elapsed_time(points):
    t = [float(p['t']) for p in points]
    if not t:
        return {'elapsed_time': None}
    return {'elapsed_time': t[-1] - t[0]}

def compute_trajectory_length(points):
    x = [float(p['x']) for p in points]
    y = [float(p['y']) for p in points]
    if len(points) < 2:
        return {'trajectory_length': 0.0}
    length = 0.0
    for i in range(1, len(points)):
        dx = x[i] - x[i-1]
        dy = y[i] - y[i-1]
        length += math.sqrt(dx*dx + dy*dy)
    return {'trajectory_length': length}

def compute_dist_end_to_end(points):
    x0, y0 = float(points[0]['x']), float(points[0]['y'])
    x1, y1 = float(points[-1]['x']), float(points[-1]['y'])
    dist = math.sqrt((x1 - x0)**2 + (y1 - y0)**2)
    return {'dist_end_to_end': dist}

def compute_theta(points):
    x = float(points[-1]['x']) - float(points[0]['x'])
    y = float(points[-1]['y']) - float(points[0]['y'])
    return {'theta': math.atan2(y, x)}

def compute_direction_from_theta(segment):
    theta = segment.get('theta')
    if theta is None:
        return {'direction': None}
    if theta < 0:
        theta += 2 * math.pi
    sector = int(theta / (math.pi / 4)) + 1
    sector = min(sector, 8)
    return {'direction': sector}

def compute_straightness(points):
    x = [float(p['x']) for p in points]
    y = [float(p['y']) for p in points]
    dist_end = math.sqrt((x[-1] - x[0])**2 + (y[-1] - y[0])**2)
    traj_length = 0
    for i in range(1, len(points)):
        dx = x[i] - x[i-1]
        dy = y[i] - y[i-1]
        traj_length += math.sqrt(dx*dx + dy*dy)
    if traj_length == 0:
        return {'straightness': 0}
    straightness = dist_end / traj_length
    return {'straightness': min(straightness, 1)}

def compute_num_points(points):
    return {'num_points': len(points)}

def compute_sum_of_angles(points):
    if len(points) < 2:
        return {'sum_of_angles': 0}
    x = [float(p['x']) for p in points]
    y = [float(p['y']) for p in points]
    angles = []
    for i in range(1, len(points)):
        dx = x[i] - x[i-1]
        dy = y[i] - y[i-1]
        angles.append(math.atan2(dy, dx))
    return {'sum_of_angles': sum(angles)}

def compute_largest_deviation(points):
    if len(points) < 3:
        return {'largest_deviation': 0}
    x0, y0 = float(points[0]['x']), float(points[0]['y'])
    xn, yn = float(points[-1]['x']), float(points[-1]['y'])
    a = xn - x0
    b = y0 - yn
    c = x0 * yn - xn * y0
    denom = math.sqrt(a**2 + b**2)
    if denom == 0:
        return {'largest_deviation': 0}
    max_dev = 0
    for p in points[1:-1]:
        x, y = float(p['x']), float(p['y'])
        dist = abs(a * x + b * y + c) / denom
        if dist > max_dev:
            max_dev = dist
    return {'largest_deviation': max_dev}

def compute_num_sharp_angles(points, TH=CURV_THRESHOLD):
    if len(points) < 3:
        return {'num_sharp_angles': 0}
    x = [float(p['x']) for p in points]
    y = [float(p['y']) for p in points]

    def angle_between(v1, v2):
        dot = v1[0]*v2[0] + v1[1]*v2[1]
        mag1 = math.sqrt(v1[0]**2 + v1[1]**2)
        mag2 = math.sqrt(v2[0]**2 + v2[1]**2)
        if mag1 == 0 or mag2 == 0:
            return 0
        cos_theta = dot / (mag1 * mag2)
        cos_theta = max(min(cos_theta, 1), -1)
        return math.acos(cos_theta)

    count = 0
    for i in range(1, len(points)-1):
        v1 = (x[i] - x[i-1], y[i] - y[i-1])
        v2 = (x[i+1] - x[i], y[i+1] - y[i])
        if angle_between(v1, v2) < TH:
            count += 1
    return {'num_sharp_angles': count}

def compute_accel_time_at_beginning(points):
    t = [float(p['t']) for p in points]
    x = [float(p['x']) for p in points]
    y = [float(p['y']) for p in points]
    if len(points) < 3:
        return {'a_beg_time': 0.0}
    v = []
    for i in range(1, len(points)):
        dt = t[i] - t[i-1] or 0.01
        dx = x[i] - x[i-1]
        dy = y[i] - y[i-1]
        v.append(math.sqrt(dx*dx + dy*dy) / dt)
    a = []
    for i in range(1, len(v)):
        dt = t[i+1] - t[i] or 0.01
        dv = v[i] - v[i-1]
        a.append(dv / dt)
    acc_time = 0.0
    for i, acc in enumerate(a):
        if acc > 0:
            if i == 0:
                acc_time += t[i+2] - t[i+1]
            else:
                acc_time += t[i+1] - t[i]
        else:
            break
    return {'a_beg_time': acc_time}

# List all feature functions
feature_functions = [
    compute_vx_stats,
    compute_vy_stats,
    compute_v_stats,
    compute_a_stats,
    compute_j_stats,
    compute_omega_stats,
    compute_curvature_stats,
    compute_elapsed_time,
    compute_trajectory_length,
    compute_dist_end_to_end,
    compute_theta,
    compute_direction_from_theta,
    compute_straightness,
    compute_num_points,
    compute_sum_of_angles,
    compute_largest_deviation,
    compute_num_sharp_angles,
    compute_accel_time_at_beginning,
]

# ----------- FEATURE EXTRACTION WRAPPER ------------

def process_segment_file(input_path, output_folder):
    # Skip if features file already exists
    output_path = os.path.join(output_folder, os.path.basename(input_path).replace('_segments.json', '_segments_with_features.json'))
    if os.path.exists(output_path):
        print(f"Features already extracted, skipping: {output_path}")
        return

    with open(input_path, 'r') as f:
        data = json.load(f)

    for segment in data['segments']:
        if isinstance(segment['points'], list) and len(segment['points']) == 1 and isinstance(segment['points'][0], list):
            points = segment['points'][0]
        else:
            points = segment['points']

        for func in feature_functions:
            if func == compute_direction_from_theta:
                stats = func(segment)
            elif func == compute_theta:
                stats = func(points)
            else:
                stats = func(points)
            segment.update(stats)

    # Flatten nested points list for final output
    for segment in data['segments']:
        if isinstance(segment['points'], list) and len(segment['points']) == 1 and isinstance(segment['points'][0], list):
            segment['points'] = segment['points'][0]

    with open(output_path, 'w') as f:
        json.dump(data, f, separators=(',', ':'))

    # Reformat JSON text for readability
    with open(output_path, 'r') as f:
        text = f.read()

    def format_points_inline(match):
        points = match.group(1)
        points = points.replace('},{', '},\n        {')
        return '[\n        ' + points + '\n      ]'

    text = re.sub(r'("segments":\[)', r'\1\n', text)
    text = re.sub(r'"points":(\[[^\]]+\])', lambda m: '"points": ' + format_points_inline(m), text)
    text = re.sub(r'(\})(,)(?=\{"type":)', r'\1\2\n', text)

    with open(output_path, 'w') as f:
        f.write(text)

    print(f"Processed and saved features for {os.path.basename(input_path)} -> {output_path}")

# ----------- MAIN PROCESSING LOOP ------------

def process_user_data(user, base_path):
    print(f"\n--- Processing user: {user} in {base_path} ---")

    base_folder = os.path.join(base_path, user)
    segment_output_folder = os.path.join(base_folder, "segment")
    features_output_folder = os.path.join(base_folder, "features_enhanced")

    os.makedirs(segment_output_folder, exist_ok=True)
    os.makedirs(features_output_folder, exist_ok=True)

    session_files = [f for f in os.listdir(base_folder) if f.startswith('session')]
    session_files.sort()

    for file_name in session_files:
        file_path = os.path.join(base_folder, file_name)
        df = pd.read_csv(file_path)

        click_mask = (df['button'] == 'Left') & (df['state'].isin(['Pressed', 'Released']))
        dup_mask = df.iloc[:, 1:].eq(df.iloc[:, 1:].shift()).all(axis=1)
        mask = dup_mask & (~click_mask)
        df_cleaned = df[~mask].reset_index(drop=True)

        df_cleaned = df_cleaned[df_cleaned['button'] != 'Scroll'].reset_index(drop=True)

        segment_and_save(df_cleaned, file_path, segment_output_folder)

    segment_files = [f for f in os.listdir(segment_output_folder) if f.endswith('_segments.json')]
    segment_files.sort()

    for filename in segment_files:
        input_path = os.path.join(segment_output_folder, filename)
        process_segment_file(input_path, features_output_folder)

    print(f"Finished processing user: {user} in {base_path}")

def main():
    for user in USERS:
        # Process training data
        process_user_data(user, BASE_TRAIN_PATH)
        # Process test data
        process_user_data(user, BASE_TEST_PATH)

if __name__ == '__main__':
    main()
