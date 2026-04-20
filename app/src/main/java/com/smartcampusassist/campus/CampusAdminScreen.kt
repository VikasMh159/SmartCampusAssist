package com.smartcampusassist.campus

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.smartcampusassist.jpui.components.DashboardHeader
import com.smartcampusassist.jpui.components.ScrollableDropdownMenuContent

private enum class AdminTab(val label: String) {
    Institutes("Institutes"),
    Students("Students"),
    Staff("Staff"),
    Academics("Classes & Subjects"),
    Assignments("Assignments")
}

private enum class ImportTarget {
    Students, Staff, Classes, Subjects
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusAdminScreen(
    navController: NavController,
    viewModel: CampusAdminViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf(AdminTab.Institutes) }
    var pendingImportTarget by remember { mutableStateOf<ImportTarget?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val target = pendingImportTarget ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        val csvText = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()

        when (target) {
            ImportTarget.Students -> viewModel.importStudents(csvText)
            ImportTarget.Staff -> viewModel.importStaff(csvText)
            ImportTarget.Classes -> viewModel.importClasses(csvText)
            ImportTarget.Subjects -> viewModel.importSubjects(csvText)
        }
    }

    BackHandler { navController.popBackStack() }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        val message = uiState.errorMessage ?: uiState.successMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Campus Admin Panel") },
                actions = {
                    TextButton(onClick = viewModel::refreshAll) { Text("Refresh") }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            if (uiState.loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    DashboardHeader()
                    AdminControlBar(
                        uiState = uiState,
                        onInstituteSelected = viewModel::selectInstitute,
                        onPageSizeSelected = viewModel::updatePageSize
                    )
                    TabRow(selectedTabIndex = selectedTab.ordinal) {
                        AdminTab.entries.forEach { tab ->
                            Tab(selected = selectedTab == tab, onClick = { selectedTab = tab }, text = { Text(tab.label) })
                        }
                    }
                    when (selectedTab) {
                        AdminTab.Institutes -> InstituteAdminTab(
                            institutes = uiState.institutes,
                            onSaveInstitute = viewModel::saveInstitute,
                            onDeleteInstitute = viewModel::deleteInstitute
                        )

                        AdminTab.Students -> StudentsAdminTab(
                            items = uiState.students,
                            currentInstituteId = uiState.selectedInstituteId,
                            branchOptions = uiState.branchOptions,
                            onSaveStudent = viewModel::saveStudent,
                            onDeleteStudent = viewModel::deleteStudent,
                            onApplyFilter = viewModel::updateStudentFilter,
                            onLoadMore = viewModel::loadMoreStudents,
                            onImportCsv = {
                                pendingImportTarget = ImportTarget.Students
                                importLauncher.launch(arrayOf("text/*"))
                            },
                            canLoadMore = uiState.canLoadMoreStudents
                        )

                        AdminTab.Staff -> StaffAdminTab(
                            items = uiState.staff,
                            departmentOptions = uiState.departmentOptions,
                            onSaveStaff = viewModel::saveStaff,
                            onDeleteStaff = viewModel::deleteStaff,
                            onApplyFilter = viewModel::updateStaffFilter,
                            onLoadMore = viewModel::loadMoreStaff,
                            onImportCsv = {
                                pendingImportTarget = ImportTarget.Staff
                                importLauncher.launch(arrayOf("text/*"))
                            },
                            canLoadMore = uiState.canLoadMoreStaff
                        )

                        AdminTab.Academics -> AcademicsAdminTab(
                            classes = uiState.classes,
                            subjects = uiState.subjects,
                            branchOptions = uiState.branchOptions,
                            onSaveClass = viewModel::saveClassRecord,
                            onDeleteClass = viewModel::deleteClassRecord,
                            onSaveSubject = viewModel::saveSubjectRecord,
                            onDeleteSubject = viewModel::deleteSubjectRecord,
                            onImportClassesCsv = {
                                pendingImportTarget = ImportTarget.Classes
                                importLauncher.launch(arrayOf("text/*"))
                            },
                            onImportSubjectsCsv = {
                                pendingImportTarget = ImportTarget.Subjects
                                importLauncher.launch(arrayOf("text/*"))
                            }
                        )

                        AdminTab.Assignments -> TeacherAssignmentTab(
                            items = uiState.assignments,
                            classOptions = uiState.classes.map { it.className }.distinct(),
                            subjectOptions = uiState.subjects.map { it.title }.distinct(),
                            branchOptions = uiState.branchOptions,
                            onSaveAssignment = viewModel::saveTeacherAssignment,
                            onDeleteAssignment = viewModel::deleteTeacherAssignment,
                            onApplyFilter = viewModel::updateAssignmentFilter,
                            onLoadMore = viewModel::loadMoreAssignments,
                            canLoadMore = uiState.canLoadMoreAssignments
                        )
                    }
                }
            }

            if (uiState.saving || uiState.importing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminControlBar(
    uiState: CampusAdminUiState,
    onInstituteSelected: (String) -> Unit,
    onPageSizeSelected: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Real deployment controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Multi-institute filtering, paginated records, classes, subjects, and CSV import flows.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InstituteSelector(
                    modifier = Modifier.weight(1f),
                    selectedInstituteId = uiState.selectedInstituteId,
                    institutes = uiState.institutes,
                    onSelected = onInstituteSelected
                )
                PageSizeSelector(
                    modifier = Modifier.weight(1f),
                    selectedPageSize = uiState.studentPageSize,
                    onSelected = onPageSizeSelected
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.institutes.forEach { institute ->
                    FilterChip(
                        selected = uiState.selectedInstituteId == institute.id,
                        onClick = { onInstituteSelected(institute.id) },
                        label = { Text(institute.name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InstituteAdminTab(
    institutes: List<CampusInstitute>,
    onSaveInstitute: (CampusInstitute) -> Unit,
    onDeleteInstitute: (String) -> Unit
) {
    var editingInstituteId by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("SALITER") }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            FormCard(title = "Add / Update Institute") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Institute Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(code, { code = it }, label = { Text("Institute Code") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(type, { type = it }, label = { Text("Type") }, supportingText = { Text("SALITER, SETI") }, modifier = Modifier.fillMaxWidth())
                    ActionRow(
                        primaryLabel = if (editingInstituteId.isBlank()) "Save Institute" else "Update Institute",
                        onPrimary = {
                            onSaveInstitute(CampusInstitute(id = editingInstituteId, name = name, code = code, type = type))
                            editingInstituteId = ""
                            name = ""
                            code = ""
                            type = "SALITER"
                        },
                        onSecondary = if (editingInstituteId.isBlank()) null else {
                            {
                                editingInstituteId = ""
                                name = ""
                                code = ""
                                type = "SALITER"
                            }
                        }
                    )
                }
            }
        }
        items(institutes, key = { it.id }) { institute ->
            MetricsCard(
                title = institute.name,
                subtitle = "${institute.type} • Code ${institute.code}",
                metrics = listOf("Students ${institute.studentCount}", "Staff ${institute.staffCount}", "Status ${institute.status}"),
                onEdit = {
                    editingInstituteId = institute.id
                    name = institute.name
                    code = institute.code
                    type = institute.type
                },
                onDelete = { onDeleteInstitute(institute.id) }
            )
        }
    }
}

@Composable
private fun StudentsAdminTab(
    items: List<StudentRecord>,
    currentInstituteId: String,
    branchOptions: List<String>,
    onSaveStudent: (StudentRecord) -> Unit,
    onDeleteStudent: (String) -> Unit,
    onApplyFilter: (StudentFilter) -> Unit,
    onLoadMore: () -> Unit,
    onImportCsv: () -> Unit,
    canLoadMore: Boolean
) {
    var editingId by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var enrollment by rememberSaveable { mutableStateOf("") }
    var branch by rememberSaveable { mutableStateOf("") }
    var semester by rememberSaveable { mutableStateOf("") }
    var division by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    var searchEnrollment by rememberSaveable { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            FormCard(title = "Student Management") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(enrollment, { enrollment = it }, label = { Text("Enrollment Number") }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SelectField(branch, branchOptions, "Branch", { branch = it }, Modifier.weight(1f))
                        OutlinedTextField(semester, { semester = it }, label = { Text("Semester") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(division, { division = it }, label = { Text("Division") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                    ActionRow(
                        primaryLabel = if (editingId.isBlank()) "Add Student" else "Update Student",
                        onPrimary = {
                            onSaveStudent(
                                StudentRecord(
                                    id = editingId,
                                    userId = enrollment.ifBlank { email },
                                    instituteId = currentInstituteId,
                                    fullName = name,
                                    enrollmentNumber = enrollment,
                                    branch = branch,
                                    semester = semester.toIntOrNull() ?: 0,
                                    division = division,
                                    email = email
                                )
                            )
                            editingId = ""
                            name = ""
                            email = ""
                            enrollment = ""
                            branch = ""
                            semester = ""
                            division = ""
                        },
                        onSecondary = { onImportCsv() },
                        secondaryLabel = "Import CSV"
                    )
                }
            }
        }
        item {
            SearchCard(title = "Student Search") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(search, { search = it }, label = { Text("Search by student name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(searchEnrollment, { searchEnrollment = it }, label = { Text("Search by enrollment number") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = {
                            onApplyFilter(StudentFilter(instituteId = currentInstituteId, query = search, enrollmentNumber = searchEnrollment))
                        }) { Text("Apply Filters") }
                        TextButton(onClick = {
                            search = ""
                            searchEnrollment = ""
                            onApplyFilter(StudentFilter(instituteId = currentInstituteId))
                        }) { Text("Reset") }
                    }
                }
            }
        }
        items(items, key = { it.id.ifBlank { it.userId } }) { student ->
            MetricsCard(
                title = student.fullName,
                subtitle = "${student.branch} • Sem ${student.semester} • Div ${student.division}",
                metrics = listOf(student.enrollmentNumber, student.email, student.instituteName.ifBlank { "Institute filtered" }),
                onEdit = {
                    editingId = student.id
                    name = student.fullName
                    email = student.email
                    enrollment = student.enrollmentNumber
                    branch = student.branch
                    semester = student.semester.toString()
                    division = student.division
                },
                onDelete = { onDeleteStudent(student.id.ifBlank { student.userId }) }
            )
        }
        if (canLoadMore) item { LoadMoreCard("Load more students", onLoadMore) }
    }
}

@Composable
private fun StaffAdminTab(
    items: List<StaffRecord>,
    departmentOptions: List<String>,
    onSaveStaff: (StaffRecord) -> Unit,
    onDeleteStaff: (String) -> Unit,
    onApplyFilter: (StaffFilter) -> Unit,
    onLoadMore: () -> Unit,
    onImportCsv: () -> Unit,
    canLoadMore: Boolean
) {
    var editingId by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var employeeId by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf(CampusRoles.TEACHER) }
    var department by rememberSaveable { mutableStateOf("") }
    var subjects by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    var roleFilter by rememberSaveable { mutableStateOf("") }
    var departmentFilter by rememberSaveable { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            FormCard(title = "Staff & Role Management") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(name, { name = it }, label = { Text("Full Name") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(employeeId, { employeeId = it }, label = { Text("Employee ID") }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SelectField(role, listOf(CampusRoles.PRINCIPAL, CampusRoles.TEACHER, CampusRoles.HOD, CampusRoles.CLERK, CampusRoles.ADMIN), "Role", { role = it }, Modifier.weight(1f))
                        SelectField(department, departmentOptions, "Department", { department = it }, Modifier.weight(1f))
                    }
                    OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(subjects, { subjects = it }, label = { Text("Subjects") }, supportingText = { Text("Comma separated or imported from CSV") }, modifier = Modifier.fillMaxWidth())
                    ActionRow(
                        primaryLabel = if (editingId.isBlank()) "Add Staff" else "Update Staff",
                        onPrimary = {
                            onSaveStaff(
                                StaffRecord(
                                    id = editingId,
                                    userId = employeeId.ifBlank { email },
                                    fullName = name,
                                    email = email,
                                    employeeId = employeeId,
                                    role = role,
                                    department = department,
                                    subjects = subjects.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                )
                            )
                            editingId = ""
                            name = ""
                            email = ""
                            employeeId = ""
                            role = CampusRoles.TEACHER
                            department = ""
                            subjects = ""
                        },
                        onSecondary = { onImportCsv() },
                        secondaryLabel = "Import CSV"
                    )
                }
            }
        }
        item {
            SearchCard(title = "Search Staff") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(search, { search = it }, label = { Text("Search by teacher or staff name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(roleFilter, { roleFilter = it }, label = { Text("Search by role") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(departmentFilter, { departmentFilter = it }, label = { Text("Search by department") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { onApplyFilter(StaffFilter(query = search, role = roleFilter, department = departmentFilter)) }) { Text("Apply Filters") }
                        TextButton(onClick = {
                            search = ""
                            roleFilter = ""
                            departmentFilter = ""
                            onApplyFilter(StaffFilter())
                        }) { Text("Reset") }
                    }
                }
            }
        }
        items(items, key = { it.id.ifBlank { it.userId } }) { staff ->
            MetricsCard(
                title = staff.fullName,
                subtitle = "${staff.role.uppercase()} • ${staff.department.ifBlank { "No department" }}",
                metrics = listOf(staff.employeeId, staff.department.ifBlank { "No department" }, staff.subjects.joinToString().ifBlank { "No subject mapping yet" }),
                onEdit = {
                    editingId = staff.id
                    name = staff.fullName
                    email = staff.email
                    employeeId = staff.employeeId
                    role = staff.role
                    department = staff.department
                    subjects = staff.subjects.joinToString()
                },
                onDelete = { onDeleteStaff(staff.id.ifBlank { staff.userId }) }
            )
        }
        if (canLoadMore) item { LoadMoreCard("Load more staff", onLoadMore) }
    }
}

@Composable
private fun AcademicsAdminTab(
    classes: List<CampusClass>,
    subjects: List<SubjectRecord>,
    branchOptions: List<String>,
    onSaveClass: (CampusClass) -> Unit,
    onDeleteClass: (String) -> Unit,
    onSaveSubject: (SubjectRecord) -> Unit,
    onDeleteSubject: (String) -> Unit,
    onImportClassesCsv: () -> Unit,
    onImportSubjectsCsv: () -> Unit
) {
    var editingClassId by rememberSaveable { mutableStateOf("") }
    var className by rememberSaveable { mutableStateOf("") }
    var classBranch by rememberSaveable { mutableStateOf("") }
    var classSemester by rememberSaveable { mutableStateOf("") }
    var classDivision by rememberSaveable { mutableStateOf("") }

    var editingSubjectId by rememberSaveable { mutableStateOf("") }
    var subjectTitle by rememberSaveable { mutableStateOf("") }
    var subjectCode by rememberSaveable { mutableStateOf("") }
    var subjectBranch by rememberSaveable { mutableStateOf("") }
    var subjectSemester by rememberSaveable { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            FormCard(title = "Class Management") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(className, { className = it }, label = { Text("Class Name") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SelectField(classBranch, branchOptions, "Branch", { classBranch = it }, Modifier.weight(1f))
                        OutlinedTextField(classSemester, { classSemester = it }, label = { Text("Semester") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(classDivision, { classDivision = it }, label = { Text("Division") }, modifier = Modifier.weight(1f))
                    }
                    ActionRow(
                        primaryLabel = if (editingClassId.isBlank()) "Save Class" else "Update Class",
                        onPrimary = {
                            onSaveClass(
                                CampusClass(
                                    id = editingClassId,
                                    branch = classBranch,
                                    semester = classSemester.toIntOrNull() ?: 0,
                                    division = classDivision,
                                    className = className
                                )
                            )
                            editingClassId = ""
                            className = ""
                            classBranch = ""
                            classSemester = ""
                            classDivision = ""
                        },
                        onSecondary = { onImportClassesCsv() },
                        secondaryLabel = "Import CSV"
                    )
                }
            }
        }
        items(classes, key = { it.id }) { campusClass ->
            MetricsCard(
                title = campusClass.className,
                subtitle = "${campusClass.branch} • Sem ${campusClass.semester} • Div ${campusClass.division}",
                metrics = listOf(campusClass.instituteName.ifBlank { "Selected institute" }),
                onEdit = {
                    editingClassId = campusClass.id
                    className = campusClass.className
                    classBranch = campusClass.branch
                    classSemester = campusClass.semester.toString()
                    classDivision = campusClass.division
                },
                onDelete = { onDeleteClass(campusClass.id) }
            )
        }
        item {
            FormCard(title = "Subject Management") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(subjectTitle, { subjectTitle = it }, label = { Text("Subject Title") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(subjectCode, { subjectCode = it }, label = { Text("Subject Code") }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SelectField(subjectBranch, branchOptions, "Branch", { subjectBranch = it }, Modifier.weight(1f))
                        OutlinedTextField(subjectSemester, { subjectSemester = it }, label = { Text("Semester") }, modifier = Modifier.weight(1f))
                    }
                    ActionRow(
                        primaryLabel = if (editingSubjectId.isBlank()) "Save Subject" else "Update Subject",
                        onPrimary = {
                            onSaveSubject(
                                SubjectRecord(
                                    id = editingSubjectId,
                                    branch = subjectBranch,
                                    semester = subjectSemester.toIntOrNull() ?: 0,
                                    code = subjectCode,
                                    title = subjectTitle
                                )
                            )
                            editingSubjectId = ""
                            subjectTitle = ""
                            subjectCode = ""
                            subjectBranch = ""
                            subjectSemester = ""
                        },
                        onSecondary = { onImportSubjectsCsv() },
                        secondaryLabel = "Import CSV"
                    )
                }
            }
        }
        items(subjects, key = { it.id }) { subject ->
            MetricsCard(
                title = subject.title,
                subtitle = "${subject.code} • ${subject.branch}",
                metrics = listOf("Semester ${subject.semester}"),
                onEdit = {
                    editingSubjectId = subject.id
                    subjectTitle = subject.title
                    subjectCode = subject.code
                    subjectBranch = subject.branch
                    subjectSemester = subject.semester.toString()
                },
                onDelete = { onDeleteSubject(subject.id) }
            )
        }
    }
}

@Composable
private fun TeacherAssignmentTab(
    items: List<TeacherAssignment>,
    classOptions: List<String>,
    subjectOptions: List<String>,
    branchOptions: List<String>,
    onSaveAssignment: (TeacherAssignment) -> Unit,
    onDeleteAssignment: (String) -> Unit,
    onApplyFilter: (AssignmentFilter) -> Unit,
    onLoadMore: () -> Unit,
    canLoadMore: Boolean
) {
    var editingId by rememberSaveable { mutableStateOf("") }
    var teacherName by rememberSaveable { mutableStateOf("") }
    var teacherId by rememberSaveable { mutableStateOf("") }
    var className by rememberSaveable { mutableStateOf("") }
    var subjectTitle by rememberSaveable { mutableStateOf("") }
    var subjectCode by rememberSaveable { mutableStateOf("") }
    var semester by rememberSaveable { mutableStateOf("") }
    var division by rememberSaveable { mutableStateOf("") }
    var branch by rememberSaveable { mutableStateOf("") }
    var searchTeacher by rememberSaveable { mutableStateOf("") }
    var searchRole by rememberSaveable { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            FormCard(title = "Teacher Multi-Class Assignment") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(teacherName, { teacherName = it }, label = { Text("Teacher Name") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(teacherId, { teacherId = it }, label = { Text("Teacher ID") }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SelectField(className, classOptions, "Class", { className = it }, Modifier.weight(1f))
                        SelectField(branch, branchOptions, "Branch", { branch = it }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SelectField(subjectTitle, subjectOptions, "Subject", { subjectTitle = it }, Modifier.weight(1f))
                        OutlinedTextField(subjectCode, { subjectCode = it }, label = { Text("Subject Code") }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(semester, { semester = it }, label = { Text("Semester") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(division, { division = it }, label = { Text("Division") }, modifier = Modifier.weight(1f))
                    }
                    ActionRow(
                        primaryLabel = if (editingId.isBlank()) "Assign Teacher" else "Update Assignment",
                        onPrimary = {
                            onSaveAssignment(
                                TeacherAssignment(
                                    id = editingId,
                                    teacherId = teacherId,
                                    teacherName = teacherName,
                                    classId = className,
                                    className = className,
                                    subjectId = subjectCode,
                                    subjectCode = subjectCode,
                                    subjectTitle = subjectTitle,
                                    semester = semester.toIntOrNull() ?: 0,
                                    division = division,
                                    branch = branch
                                )
                            )
                            editingId = ""
                            teacherName = ""
                            teacherId = ""
                            className = ""
                            subjectTitle = ""
                            subjectCode = ""
                            semester = ""
                            division = ""
                            branch = ""
                        }
                    )
                }
            }
        }
        item {
            SearchCard(title = "Assignment Search & Filters") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(searchTeacher, { searchTeacher = it }, label = { Text("Search by teacher name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(searchRole, { searchRole = it }, label = { Text("Search by role") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { onApplyFilter(AssignmentFilter(teacherName = searchTeacher, role = searchRole)) }) { Text("Apply Filters") }
                        TextButton(onClick = {
                            searchTeacher = ""
                            searchRole = ""
                            onApplyFilter(AssignmentFilter())
                        }) { Text("Reset") }
                    }
                }
            }
        }
        items(items, key = { it.id }) { assignment ->
            MetricsCard(
                title = assignment.teacherName,
                subtitle = "${assignment.subjectCode} • ${assignment.subjectTitle}",
                metrics = listOf(assignment.className, "Sem ${assignment.semester} Div ${assignment.division}", assignment.branch),
                onEdit = {
                    editingId = assignment.id
                    teacherName = assignment.teacherName
                    teacherId = assignment.teacherId
                    className = assignment.className
                    subjectTitle = assignment.subjectTitle
                    subjectCode = assignment.subjectCode
                    semester = assignment.semester.toString()
                    division = assignment.division
                    branch = assignment.branch
                },
                onDelete = { onDeleteAssignment(assignment.id) }
            )
        }
        if (canLoadMore) item { LoadMoreCard("Load more assignments", onLoadMore) }
    }
}

@Composable
private fun FormCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun SearchCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun MetricsCard(
    title: String,
    subtitle: String,
    metrics: List<String>,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            metrics.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (onEdit != null || onDelete != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    onEdit?.let { TextButton(onClick = it) { Text("Edit") } }
                    onDelete?.let { TextButton(onClick = it) { Text("Delete") } }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    primaryLabel: String,
    onPrimary: () -> Unit,
    onSecondary: (() -> Unit)? = null,
    secondaryLabel: String = "Cancel"
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = onPrimary) { Text(primaryLabel) }
        onSecondary?.let { TextButton(onClick = it) { Text(secondaryLabel) } }
    }
}

@Composable
private fun LoadMoreCard(label: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontWeight = FontWeight.Bold)
            Button(onClick = onClick) { Text("Load More") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstituteSelector(
    modifier: Modifier = Modifier,
    selectedInstituteId: String,
    institutes: List<CampusInstitute>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = institutes.firstOrNull { it.id == selectedInstituteId }?.name.orEmpty()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Institute") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ScrollableDropdownMenuContent(items = institutes) { institute ->
                DropdownMenuItem(
                    text = { Text(institute.name) },
                    onClick = {
                        expanded = false
                        onSelected(institute.id)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectField(
    value: String,
    options: List<String>,
    label: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val resolvedOptions = options.filter { it.isNotBlank() }.distinct().sorted()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onSelected,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ScrollableDropdownMenuContent(items = resolvedOptions) { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageSizeSelector(
    modifier: Modifier = Modifier,
    selectedPageSize: Int,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(20, 50)
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = selectedPageSize.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Page Size") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ScrollableDropdownMenuContent(items = options) { size ->
                DropdownMenuItem(
                    text = { Text(size.toString()) },
                    onClick = {
                        expanded = false
                        onSelected(size)
                    }
                )
            }
        }
    }
}
