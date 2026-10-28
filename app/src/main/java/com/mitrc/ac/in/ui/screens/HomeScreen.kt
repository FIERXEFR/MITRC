package com.mitrc.ac.`in`.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mitrc.ac.`in`.auth.AuthRepository
import com.mitrc.ac.`in`.data.PortalRepository
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.TextPrimary
import com.mitrc.ac.`in`.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class StaffRole {
    STUDENT, TEACHER, COORDINATOR, NOT_LINKED, LOADING
}

private const val TAG = "HomeScreen"

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

        runCatching {
            PortalRepository.linkMyFacultyAccount()
        }.onFailure {
            Log.w(TAG, "link_my_faculty_account failed: ${it.message}")
        }

        val userEmail = user?.email.orEmpty()
        Log.d(TAG, "Resolving role for userUid=$userUid, email=$userEmail")

        val portals = PortalRepository.getMyPortals().getOrNull()
        Log.d(TAG, "my_portals: $portals")

        // 1. Check if user is a Coordinator FIRST
        if (portals?.isCoordinator == true) {
            Log.i(TAG, "Role resolved as COORDINATOR via my_portals")
            resolvedRole = StaffRole.COORDINATOR
            return@LaunchedEffect
        }

        val myClassesRes = PortalRepository.getMyCoordinatorClasses()
        val myClasses = myClassesRes.getOrNull()
        Log.d(TAG, "v_my_coordinator_classes rows=${myClasses?.size}")
        if (!myClasses.isNullOrEmpty()) {
            Log.i(TAG, "Role resolved as COORDINATOR via v_my_coordinator_classes (${myClasses.size} classes)")
            resolvedRole = StaffRole.COORDINATOR
            return@LaunchedEffect
        }

        val coordsRes = PortalRepository.getCoordinatorsList()
        val coordinatorMatch = coordsRes.getOrNull()?.firstOrNull { c ->
            (!c.email.isNullOrBlank() && c.email.equals(userEmail, ignoreCase = true))
        }
        if (coordinatorMatch != null) {
            Log.i(TAG, "Role resolved as COORDINATOR via v_coordinators match (${coordinatorMatch.course})")
            resolvedRole = StaffRole.COORDINATOR
            return@LaunchedEffect
        }

        val staffRow = PortalRepository.getStaffDirectory().getOrNull()?.firstOrNull { row ->
            (!row.firebaseUid.isNullOrBlank() && row.firebaseUid == userUid) ||
                (!row.email.isNullOrBlank() && row.email.equals(userEmail, ignoreCase = true))
        }
        if (staffRow != null && staffRow.isCoordinator) {
            Log.i(TAG, "Role resolved as COORDINATOR via v_staff_directory (isCoordinator=true)")
            resolvedRole = StaffRole.COORDINATOR
            return@LaunchedEffect
        }

        // 2. Check if user is a Teacher / Faculty SECOND
        if (portals?.isTeacher == true || portals?.isFaculty == true || portals?.isMentor == true || portals?.isHod == true) {
            Log.i(TAG, "Role resolved as TEACHER via my_portals")
            resolvedRole = StaffRole.TEACHER
            return@LaunchedEffect
        }

        val facultyRow = PortalRepository.getFacultyByUidOrEmail(userUid, userEmail).getOrNull()
        if (facultyRow != null) {
            Log.i(TAG, "Role resolved as TEACHER via faculty table match (id=${facultyRow.id})")
            if (facultyRow.firebaseUid.isNullOrBlank() && userEmail.isNotBlank()) {
                PortalRepository.linkFacultyUidByEmail(userUid, userEmail)
            }
            resolvedRole = StaffRole.TEACHER
            return@LaunchedEffect
        }

        if (staffRow != null) {
            Log.i(TAG, "Role resolved as TEACHER via v_staff_directory")
            resolvedRole = StaffRole.TEACHER
            return@LaunchedEffect
        }

        // 3. Check Student Profile
        val student = PortalRepository.getStudentProfile(userUid).getOrNull()
        if (student != null) {
            Log.i(TAG, "Role resolved as STUDENT via student profile")
            resolvedRole = StaffRole.STUDENT
            return@LaunchedEffect
        }

        Log.w(TAG, "No role matched for uid=$userUid, email=$userEmail. Blocking portal access")
        resolvedRole = StaffRole.NOT_LINKED
    }

    when (resolvedRole) {
        StaffRole.LOADING -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Gold)
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
        StaffRole.NOT_LINKED -> {
            NotLinkedScreen(onSignedOut = onSignedOut)
        }
    }
}

@Composable
private fun NotLinkedScreen(onSignedOut: () -> Unit) {
    val scope = rememberCoroutineScope()
    var signingOut by remember { mutableStateOf(false) }

    // Unlinked accounts never reach a portal: sign them out automatically within 3 seconds.
    LaunchedEffect(Unit) {
        delay(2500)
        if (!signingOut) {
            signingOut = true
            AuthRepository.signOut()
            onSignedOut()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Account not linked",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Your account isn't linked to any student or staff record. You'll be signed out automatically. Contact the admin panel to activate your ID.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        if (signingOut) {
            CircularProgressIndicator(
                color = Gold,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Signing out...",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        } else {
            Button(
                onClick = {
                    signingOut = true
                    scope.launch {
                        AuthRepository.signOut()
                        onSignedOut()
                    }
                },
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Sign Out Now")
            }
        }
    }
}
