package tracker.input.win

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.*

object Native {
    val user32: User32 = Native.load("user32", User32::class.java)
}

interface User32 : Library {
    fun GetCursorPos(point: POINT): BOOL
    fun GetAsyncKeyState(vKey: Int): SHORT
    fun GetForegroundWindow(): HWND
    fun GetWindowTextW(hWnd: HWND, lpString: CharArray, nMaxCount: Int): Int

    fun SystemParametersInfoW(
        uiAction: Int,
        uiParam: Int,
        pvParam: Pointer,
        fWinIni: Int
    ): Boolean
}
