package com.example.valentinesgarage.Screens.Employee

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

// --- Data model (replace with ViewModel state) ---

data class EmployeeProfile(
    val name: String,
    val role: String,
    val employeeId: String,
    val email: String,
    val phone: String,
    val department: String,
    val shift: String,
    val joinDate: String,
    val tasksCompleted: Int,
    val tasksInProgress: Int,
    val tasksPending: Int
)

private val sampleEmployee = EmployeeProfile(
    name = "John Mutanga",
    role = "Senior Mechanic",
    employeeId = "EMP-00142",
    email = "j.mutanga@valentinesgarage.com",
    phone = "+264 81 234 5678",
    department = "Engine & Drivetrain",
    shift = "Morning  |  06:00 – 14:00",
    joinDate = "12 March 2021",
    tasksCompleted = 128,
    tasksInProgress = 3,
    tasksPending = 5
)

// --- Colour palette ---
private val DarkInk    = Color(0xFF111318)

private val DividerGray = Color(0xFFE4E4E7)
private val AccentOrange = Color(0xFFE8500A)
private val SubtleGray  = Color(0xFF8A8A96)

// --- Screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeProfilePage(navController: NavController) {
    val employee = sampleEmployee   // swap with viewModel.employee.collectAsState()

    Scaffold(
        containerColor = Color(0xFFF2F2F4),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = DarkInk
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: edit profile */ }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = DarkInk
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ── Hero Header ──────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()

                        .padding(top = 0.dp, bottom = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(86.dp)
                                .clip(CircleShape)
                                .background(AccentOrange.copy(alpha = 0.15f))
                                .border(2.dp, AccentOrange, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = employee.name
                                    .split(" ")
                                    .mapNotNull { it.firstOrNull()?.uppercase() }
                                    .take(2)
                                    .joinToString(""),
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }

                        Text(
                            text = employee.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = AccentOrange.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = employee.role,
                                color = AccentOrange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                            )
                        }

                        Text(
                            text = employee.employeeId,
                            fontSize = 12.sp,
                            color = Color(0xFF000000),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // ── Stats Row ────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatChip(
                        modifier = Modifier.weight(1f),
                        value = employee.tasksCompleted.toString(),
                        label = "Done",
                        color = Color(0xFF2E7D32)
                    )
                    StatChip(
                        modifier = Modifier.weight(1f),
                        value = employee.tasksInProgress.toString(),
                        label = "Active",
                        color = Color(0xFF1565C0)
                    )
                    StatChip(
                        modifier = Modifier.weight(1f),
                        value = employee.tasksPending.toString(),
                        label = "Pending",
                        color = Color(0xFFE65100)
                    )
                }
            }

            // ── Contact Info ─────────────────────────────────────────
            item {
                ProfileSection(title = "Contact Information") {
                    InfoRow(Icons.Default.Email,  "Email",  employee.email)
                    SectionDivider()
                    InfoRow(Icons.Default.Phone,  "Phone",  employee.phone)
                }
            }

            // ── Work Details ─────────────────────────────────────────
            item {
                ProfileSection(title = "Work Details") {
                    InfoRow(Icons.Default.Build,     "Department", employee.department)
                    SectionDivider()
                    InfoRow(Icons.Default.Schedule,  "Shift",      employee.shift)
                    SectionDivider()
                    InfoRow(Icons.Default.DateRange, "Joined",     employee.joinDate)
                }
            }

            // ── Sign Out ─────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = {
                        // Log out logic: Navigate to Login and clear backstack
                        navController.navigate("Login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(listOf(Color(0xFFD32F2F), Color(0xFFD32F2F)))
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign Out",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// --- Reusable components ---

@Composable
private fun StatChip(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = SubtleGray,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProfileSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = SubtleGray,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFFF0F0F2)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DarkInk,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = SubtleGray,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkInk
            )
        }
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 66.dp, end = 16.dp),
        color = DividerGray,
        thickness = 0.8.dp
    )
}

@Preview(showBackground = true)
@Composable
fun EmployeeProfilePreview() {
    EmployeeProfilePage(navController = rememberNavController())
}