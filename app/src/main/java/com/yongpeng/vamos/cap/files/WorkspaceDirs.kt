// Modified by Vamos (2026): private conversation workspace directories.
package com.yongpeng.vamos.cap.files
import android.content.Context
import com.yongpeng.vamos.core.workspace.*
import java.io.File
import java.security.MessageDigest
class WorkspaceDirs(val root:File,val gate:WorkspaceFileGate,val tree:WorkspaceTree) {
 constructor(context:Context):this(File(context.filesDir,"workspaces"),AndroidFdSys.gate,WorkspaceOwner.of(context).workspaceTree)
 val work get() = File(root,"work")
 fun workspace(id:String):File = File(work,if(Regex("^[A-Za-z0-9_-]{1,100}$").matches(id)) "c-$id" else "h-"+MessageDigest.getInstance("SHA-256").digest(id.toByteArray()).joinToString(""){"%02x".format(it)}.take(32))
}
