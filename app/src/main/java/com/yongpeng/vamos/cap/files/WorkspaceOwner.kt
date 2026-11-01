// Modified by Vamos (2026): shared lock for private workspace files.
package com.yongpeng.vamos.cap.files
import android.content.Context
import com.yongpeng.vamos.core.workspace.WorkspaceTree
interface WorkspaceOwner {
 val workspaceTree:WorkspaceTree
 companion object { fun of(context:Context) = context.applicationContext as WorkspaceOwner }
}
