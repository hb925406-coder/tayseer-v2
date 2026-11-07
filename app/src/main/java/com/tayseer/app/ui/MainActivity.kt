package com.tayseer.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tayseer.app.data.AttendanceStatus
import com.tayseer.app.data.Student
import com.tayseer.app.data.UserRole

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val currentDate by viewModel.currentDate.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()

    val roleName = when (currentUserRole) {
        UserRole.ADMIN -> "الإدارة المدرسية"
        UserRole.TEACHER -> "معلم"
        UserRole.PARENT -> "ولي أمر"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "تطبيق تيسير الميداني",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "التاريخ: $currentDate | الدور: $roleName",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1B5E20))
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = "التحضير") },
                    label = { Text("التحضير") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "الطلاب") },
                    label = { Text("الطلاب") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "المتابعة") },
                    label = { Text("المتابعة") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> AttendanceTab(viewModel)
                1 -> CenterMessage("شاشة إدارة وتصنيف الطلاب")
                2 -> CenterMessage("شاشة متابعة الأعذار والتنبيهات")
            }
        }
    }
}

@Composable
fun AttendanceTab(viewModel: MainViewModel) {
    val students by viewModel.allStudents.collectAsState()
    val dailyAttendance by viewModel.dailyAttendance.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.markAllPresent(students) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("الكل حاضر", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "قائمة الرصد اليومي (${students.size} طالب)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(students, key = { it.id }) { student ->
                val currentStatus = dailyAttendance[student.id] ?: AttendanceStatus.PRESENT
                StudentCard(
                    student = student,
                    currentStatus = currentStatus,
                    onStatusSelected = { status ->
                        viewModel.updateStudentAttendance(student.id, status)
                    }
                )
            }
        }
    }
}

@Composable
fun StudentCard(
    student: Student,
    currentStatus: AttendanceStatus,
    onStatusSelected: (AttendanceStatus) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = student.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${student.grade} - شعبة ${student.section}",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )

            val row1 = listOf(
                Triple(AttendanceStatus.PRESENT, "حاضر", Color(0xFF2E7D32)),
                Triple(AttendanceStatus.ABSENT, "غائب", Color(0xFFC62828)),
                Triple(AttendanceStatus.EXCUSED, "مستأذن", Color(0xFF1565C0))
            )
            val row2 = listOf(
                Triple(AttendanceStatus.LATE, "متأخر", Color(0xFFEF6C00)),
                Triple(AttendanceStatus.ABSENT_WITH_EXCUSE, "غائب بعذر", Color(0xFF6A1B9A)),
                Triple(AttendanceStatus.ESCAPED, "هارب", Color(0xFF424242))
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row1.forEach { (status, label, activeColor) ->
                        StatusButton(
                            label = label,
                            isSelected = currentStatus == status,
                            activeColor = activeColor,
                            modifier = Modifier.weight(1f),
                            onClick = { onStatusSelected(status) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row2.forEach { (status, label, activeColor) ->
                        StatusButton(
                            label = label,
                            isSelected = currentStatus == status,
                            activeColor = activeColor,
                            modifier = Modifier.weight(1f),
                            onClick = { onStatusSelected(status) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusButton(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) activeColor else Color(0xFFF1F3F4))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color.Black
        )
    }
}

@Composable
fun CenterMessage(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 16.sp, color = Color.Gray)
    }
}
