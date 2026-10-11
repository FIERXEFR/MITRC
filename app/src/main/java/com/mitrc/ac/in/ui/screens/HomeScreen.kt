package com.mitrc.ac.`in`.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mitrc.ac.`in`.auth.AuthRepository
import com.mitrc.ac.`in`.data.PortalRepository
import com.mitrc.ac.`in`.ui.theme.Navy

enum class StaffRole {
    STUDENT, TEACHER, COORDINATOR, LOADING
}

/**
 * Smart Router for the MITRC App.
 * Detects role from Supabase schema and delegates to the appropriate Portal Shell.
 */
@Composable
fun HomeScreen(onSignedOut: () -> Unit) {
    val user = AuthRepository.currentUser
    val userUid = user?.uid.orEmpty()
    var resolvedRole by remember { mutableStateOf(StaffRole.LOADING) }

    LaunchedEffect(userUid) {
        if (userUid.isEmpty()) {
            onSignedOut()
            return@LaunchedEffect
        }

        // 1. If faculty, link Firebase account to faculty_master by verified email (ignore error for students/admins)
        runCatching {
            PortalRepository.linkMyFacultyAccount()
        }

        // 2. Fetch my_portals RPC to inspect all role flags
        val portals = PortalRepository.getMyPortals().getOrNull()

        if (portals != null) {
            when {
                portals.isCoordinator -> {
                    resolvedRole = StaffRole.COORDINATOR
                    return@LaunchedEffect
                }
                portals.isTeacher || portals.isFaculty || portals.isMentor || portals.isHod -> {
                    resolvedRole = StaffRole.TEACHER
                    return@LaunchedEffect
                }
                portals.isStudent -> {
                    resolvedRole = StaffRole.STUDENT
                    return@LaunchedEffect
                }
            }
        }

        // 3. Fallback: Check for Student Profile directly
        val student = PortalRepository.getStudentProfile(userUid).getOrNull()
        if (student != null) {
            resolvedRole = StaffRole.STUDENT
            return@LaunchedEffect
        }

        // Fallback or No profile found: Student portal
        resolvedRole = StaffRole.STUDENT 
    }

    when (resolvedRole) {
        StaffRole.LOADING -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Navy)
            }
        }
        StaffRole.STUDENT -> {
            StudentPortalShell(userUid = userUid, onSignedOut = onSignedOut)
        }
        StaffRole.TEACHER -> {
            TeacherPortalShell(userUid = userUid, onSignedOut = onSignedOut)
        }
        StaffRole.COORDINATOR -> {
            CoordinatorPortalShell(userUid = userUid, onSignedOut = onSignedOut)
        }
    }
}
