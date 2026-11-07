package com.tayseer.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
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
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(
                    colorScheme = lightColorScheme(
                        primary = Color(0xFF1B5E20),
                        secondary = Color(0xFF2E7D32),
                        background = Color(0xFFF9FBF9)
                    )
                ) {
                    TayseerMainScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TayseerMainScreen(viewModel: MainViewModel) {
    val currentRole by viewModel.currentUserRole.collectAsState()
    val allStudents by viewModel.allStudents.collectAsState()
    val dailyAttendance by viewModel.dailyAttendance.collectAsState()
    val pendingAlerts by viewModel.pendingAlerts.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()

    var showAddStudentDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importStudentsFromFile(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("تطبيق تيسير الميداني", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            text = "التاريخ: $currentDate | الدور: ${getRoleTitle(currentRole)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = {
                        val nextRole = when (currentRole) {
                            UserRole.ADMIN -> UserRole.TEACHER
                            UserRole.TEACHER -> UserRole.PARENT
                            UserRole.PARENT -> UserRole.ADMIN
                        }
                        viewModel.setUserRole(nextRole)
                    }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "تغيير الدور")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                    label = { Text("التحضير") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.People, contentDescription = null) },
                    label = { Text("الطلاب") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        BadgedBox(badge = {
                            if (pendingAlerts.isNotEmpty()) {
                                Badge { Text(pendingAlerts.size.toString()) }
                            }
                        }) {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                        }
                    },
                    label = { Text("المتابعة") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 1 && currentRole == UserRole.ADMIN) {
                FloatingActionButton(
                    onClick = { showAddStudentDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة طالب")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF6F8F6))
        ) {
            when (selectedTab) {
                0 -> AttendanceTabContent(
                    students = allStudents,
                    attendanceMap = dailyAttendance,
                    onStatusChange = { studentId, status ->
                        viewModel.updateStudentAttendance(studentId, status)
                    },
                    onMarkAllPresent = { viewModel.markAllPresent(allStudents) }
                )
                1 -> StudentsTabContent(
                    students = allStudents,
                    onImportClick = {
                        filePickerLauncher.launch(
                            arrayOf(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "text/csv",
                                "text/comma-separated-values"
                            )
                        )
                    }
                )
                2 -> AlertsTabContent(
                    alerts = pendingAlerts,
                    onResolve = { id, method, note ->
                        viewModel.resolveAlert(id, method, note)
                    }
                )
            }
        }
    }

    if (showAddStudentDialog) {
        AddStudentDialog(
            onDismiss = { showAddStudentDialog = false },
            onConfirm = { name, grade, section, phone ->
                viewModel.addStudent(
                    Student(name = name, grade = grade, section = section, parentPhone = phone)
                )
                showAddStudentDialog = false
            }
        )
    }
}

@Composable
fun AttendanceTabContent(
    students: List<Student>,
    attendanceMap: Map<Long, AttendanceStatus>,
    onStatusChange: (Long, AttendanceStatus) -> Unit,
    onMarkAllPresent: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "قائمة الرصد اليومي (${students.size} طالب)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Button(
                onClick = onMarkAllPresent,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("الكل حاضر", fontSize = 13.sp)
            }
        }

        if (students.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا يوجد طلاب مسجلون حالياً. تفضل باستيرادهم أو إضافتهم.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(students, key = { it.id }) { student ->
                    val status = attendanceMap[student.id] ?: AttendanceStatus.PRESENT
                    AttendanceCard(student = student, currentStatus = status, onStatusChange = { newStatus ->
                        onStatusChange(student.id, newStatus)
                    })
                }
            }
        }
    }
}

@Composable
fun AttendanceCard(
    student: Student,
    currentStatus: AttendanceStatus,
    onStatusChange: (AttendanceStatus) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = student.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                text = "${student.grade} - شعبة (${student.section})",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusChip("حاضر", AttendanceStatus.PRESENT, currentStatus, Color(0xFF2E7D32), onStatusChange)
                StatusChip("غائب", AttendanceStatus.ABSENT, currentStatus, Color(0xFFD32F2F), onStatusChange)
                StatusChip("مستأذن", AttendanceStatus.EXCUSED, currentStatus, Color(0xFF0288D1), onStatusChange)
                StatusChip("متأخر", AttendanceStatus.LATE, currentStatus, Color(0xFFF57C00), onStatusChange)
                StatusChip("هارب", AttendanceStatus.ESCAPED, currentStatus, Color(0xFF7B1FA2), onStatusChange)
            }
        }
    }
}

@Composable
fun StatusChip(
    label: String,
    status: AttendanceStatus,
    selectedStatus: AttendanceStatus,
    activeColor: Color,
    onSelect: (AttendanceStatus) -> Unit
) {
    val isSelected = status == selectedStatus
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) activeColor else Color(0xFFF0F0F0),
        modifier = Modifier
            .clickable { onSelect(status) }
            .padding(horizontal = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color.Black,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun StudentsTabContent(students: List<Student>, onImportClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("إدارة الطلاب المسجلين", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            OutlinedButton(onClick = onImportClick) {
                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("استيراد Excel / CSV", fontSize = 12.sp)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(students, key = { it.id }) { student ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(student.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("${student.grade} - ${student.section}", fontSize = 12.sp, color = Color.Gray)
                        }
                        if (student.parentPhone.isNotBlank()) {
                            Text(student.parentPhone, fontSize = 12.sp, color = Color(0xFF1B5E20))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlertsTabContent(
    alerts: List<com.tayseer.app.data.AttendanceRecord>,
    onResolve: (Long, String, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("تنبيهات الغياب والمتابعة المعلقة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        if (alerts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("رائع! لا توجد حالات غياب أو هروب معلقة بدون متابعة.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(alerts, key = { it.id }) { record ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("تنبيه حالة: ${record.statusEnum.label}", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                            Text("التاريخ: ${record.date}", fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onResolve(record.id, "اتصال هاتفي", "تم التواصل مع ولي الأمر") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("اتصال", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { onResolve(record.id, "واتساب", "أرسلت رسالة عبر واتساب") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("واتساب", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { onResolve(record.id, "عذر ورقي", "تم استلام ورقة عذر") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("عذر ورقي", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddStudentDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("الصف الأول") }
    var section by remember { mutableStateOf("أ") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة طالب جديد", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الطالب الرباعي") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = grade,
                    onValueChange = { grade = it },
                    label = { Text("الصف / المستوى") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = section,
                    onValueChange = { section = it },
                    label = { Text("الشعبة / الحلقة") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم هاتف ولي الأمر") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, grade, section, phone) },
                enabled = name.isNotBlank()
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

fun getRoleTitle(role: UserRole): String {
    return when (role) {
        UserRole.ADMIN -> "الإدارة المدرسية"
        UserRole.TEACHER -> "المعلم"
        UserRole.PARENT -> "ولي الأمر"
    }
}
