package tracker.input.ui

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.*
import com.sun.jna.platform.win32.COM.COMUtils
import com.sun.jna.platform.win32.COM.IUnknown
import com.sun.jna.platform.win32.COM.Unknown
import com.sun.jna.ptr.PointerByReference
import tracker.data.StateSnapshot
import tracker.input.win.Native
import java.lang.reflect.Constructor

interface IUIAutomation : IUnknown {
    fun ElementFromPoint(pt: WinDef.POINT, element: PointerByReference): WinNT.HRESULT
}

interface IUIAutomationElement : IUnknown {
    fun get_CurrentBoundingRectangle(rect: WinDef.RECT): WinNT.HRESULT
    fun get_CurrentName(name: PointerByReference): WinNT.HRESULT
}

object UIAutomationHelper {
    private val CLSID_CUIAutomation = Guid.CLSID("{FF48DBA4-60EF-4201-AA87-54103EEF594E}")
    private val IID_IUIAutomation = Guid.IID("{30CBE57D-D9D0-452A-AB13-7AC5AC4825EE}")

    fun getElementAtCursor(): Pair<String, WinDef.RECT>? {
        Ole32.INSTANCE.CoInitializeEx(Pointer.NULL, Ole32.COINIT_MULTITHREADED)

        val automationRef = PointerByReference()
        val hr = Ole32.INSTANCE.CoCreateInstance(
            CLSID_CUIAutomation,
            null,
            WTypes.CLSCTX_INPROC_SERVER,
            IID_IUIAutomation,
            automationRef
        )
        if (COMUtils.FAILED(hr.intValue()) || automationRef.value == null) return null

        val automation = queryInterfaceFromPointer<IUIAutomation>(automationRef.value, IID_IUIAutomation) ?: return null

        val cursorPos = WinDef.POINT()
        Native.user32.GetCursorPos(cursorPos)

        val elementRef = PointerByReference()
        if (COMUtils.FAILED(automation.ElementFromPoint(cursorPos, elementRef).intValue()) || elementRef.value == null) return null

        val element = queryInterfaceFromPointer<IUIAutomationElement>(elementRef.value, Guid.IID("{D22108AA-8AC5-49A5-837B-37BBB3D7591E}")) ?: return null

        val nameRef = PointerByReference()
        if (COMUtils.FAILED(element.get_CurrentName(nameRef).intValue()) || nameRef.value == null) return null

        val name = nameRef.value.getWideString(0)
        val rect = WinDef.RECT()
        val rectResult = element.get_CurrentBoundingRectangle(rect)
        if (COMUtils.FAILED(rectResult.intValue())) return null

        return name to rect
    }

    fun enrichSnapshot(snapshot: StateSnapshot): StateSnapshot {
        val uiInfo = getElementAtCursor()
        return if (uiInfo != null) {
            val (name, rect) = uiInfo
            snapshot.copy(windowTitle = "$name [${rect.left},${rect.top},${rect.right},${rect.bottom}]")
        } else {
            snapshot
        }
    }

    inline fun <reified T : IUnknown> queryInterfaceFromPointer(ptr: Pointer, iid: Guid.GUID): T? {
        val unknown = Unknown(ptr)
        val out = PointerByReference()
        val hr = unknown.QueryInterface(iid, out)
        if (COMUtils.FAILED(hr.intValue()) || out.value == null) return null
        val ctor: Constructor<T> = T::class.java.getConstructor(Pointer::class.java)
        return ctor.newInstance(out.value)
    }
}
