package com.tayseer.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import com.tayseer.app.data.*

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
    var showAddStudentDialog by remember { mutableStateOf(false) }

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
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showAddStudentDialog = true },
                    containerColor = Color(0xFF1B5E20),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة طالب")
                }
            }
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
                1 -> StudentsTab(viewModel)
                2 -> FollowUpTab(viewModel)
            }
        }

        if (showAddStudentDialog) {
            AddStudentDialog(
                onDismiss = { showAddStudentDialog = false },
                onConfirm = { name, grade, section ->
                    viewModel.addStudent(
                        Student(
                            name = name,
                            grade = grade,
                            section = section
                        )
                    )
                    showAddStudentDialog = false
                }
            )
        }
    }
}

@Composable
fun AttendanceTab(viewModel: MainViewModel) {
    val students by viewModel.allStudents.collectAsState()
    val dailyAttendance by viewModel.dailyAttendance.collectAsState()
    val todayEvaluations by viewModel.todayEvaluations.collectAsState()

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
                text = "قائمة الرصد والتقييم (${students.size} طالب)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (students.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا يوجد طلاب مسجلين، اضغط على زر (+) لإضافة طالب", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(students, key = { it.id }) { student ->
                    val currentStatus = dailyAttendance[student.id] ?: AttendanceStatus.PRESENT
                    val studentEvaluations = todayEvaluations.filter { it.studentId == student.id }
                    
                    StudentCard(
                        student = student,
                        currentStatus = currentStatus,
                        studentEvaluations = studentEvaluations,
                        onStatusSelected = { status ->
                            viewModel.updateStudentAttendance(student.id, status)
                        },
                        onAddEvaluation = { evalType ->
                            viewModel.addEvaluation(student.id, evalType)
                        },
                        onDeleteEvaluation = { evalRecord ->
                            viewModel.deleteEvaluation(evalRecord)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StudentCard(
    student: Student,
    currentStatus: AttendanceStatus,
    studentEvaluations: List<EvaluationRecord>,
    onStatusSelected: (AttendanceStatus) -> Unit,
    onAddEvaluation: (EvaluationType) -> Unit,
    onDeleteEvaluation: (EvaluationRecord) -> Unit
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
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )

            // شبكة حالات الحضور الست
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

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // قسم التقييمات السلوكية والأكاديمية
            Text(
                text = "التقييم اليومي السريع:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )

            val evaluationOptions = listOf(
                EvaluationType.MEMORIZED to "حفظه ⭐",
                EvaluationType.NOT_MEMORIZED to "لم يحفظ ⚠️",
                EvaluationType.DISTINGUISHED to "مميز 🏅",
                EvaluationType.POLITE to "مؤدب 🌿",
                EvaluationType.FORGOT_BOOKS to "نسيان الكتب 📖"
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                items(evaluationOptions) { (evalType, title) ->
                    SuggestionChip(
                        onClick = { onAddEvaluation(evalType) },
                        label = { Text(title, fontSize = 11.sp) }
                    )
                }
            }

            // عرض التقييمات المسجلة للطالب اليوم مع إمكانية حذفها
            if (studentEvaluations.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    items(studentEvaluations) { eval ->
                        val chipBg = if (eval.type.isPositive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        val chipText = if (eval.type.isPositive) Color(0xFF2E7D32) else Color(0xFFC62828)

                        AssistChip(
                            onClick = { onDeleteEvaluation(eval) },
                            label = { Text("${eval.type.labelAr} ✕", fontSize = 11.sp, color = chipText) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = chipBg)
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
fun StudentsTab(viewModel: MainViewModel) {
    val students by viewModel.allStudents.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "دليل الطلاب المسجلين (${students.size} طالب)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (students.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لم يتم إضافة أي طالب بعد", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(students, key = { it.id }) { student ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(student.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text("${student.grade} - شعبة ${student.section}", fontSize = 12.sp, color = Color.Gray)
                            }
                            Text("رقم القيد: #${student.id}", fontSize = 11.sp, color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FollowUpTab(viewModel: MainViewModel) {
    val pendingAlerts by viewModel.pendingAlerts.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "سجل التنبيهات والأعذار (${pendingAlerts.size})",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (pendingAlerts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد حالات غياب أو تأخير تتطلب المتابعة حالياً", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(pendingAlerts, key = { it.id }) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "حالة رصد: ${record.statusEnum.name} - تاريخ: ${record.date}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.resolveAlert(record.id, "واتساب", "تم التواصل مع ولي الأمر") },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("توثيق واتساب", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { viewModel.resolveAlert(record.id, "ورقة عذر", "تم استلام عذر رسمي") },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("ورقة عذر", fontSize = 11.sp)
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
fun AddStudentDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, grade: String, section: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("الصف الأول") }
    var section by remember { mutableStateOf("أ") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة طالب جديد", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الطالب الرباعي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = grade,
                    onValueChange = { grade = it },
                    label = { Text("الصف / المستوى") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = section,
                    onValueChange = { section = it },
                    label = { Text("الشعبة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), grade.trim(), section.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
            ) {
                Text("إضافة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
