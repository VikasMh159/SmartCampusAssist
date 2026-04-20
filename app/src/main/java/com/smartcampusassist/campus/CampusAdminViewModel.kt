package com.smartcampusassist.campus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CampusAdminUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val importing: Boolean = false,
    val institutes: List<CampusInstitute> = emptyList(),
    val students: List<StudentRecord> = emptyList(),
    val staff: List<StaffRecord> = emptyList(),
    val assignments: List<TeacherAssignment> = emptyList(),
    val classes: List<CampusClass> = emptyList(),
    val subjects: List<SubjectRecord> = emptyList(),
    val departmentOptions: List<String> = emptyList(),
    val branchOptions: List<String> = emptyList(),
    val selectedInstituteId: String = "",
    val selectedInstituteName: String = "",
    val studentPageSize: Int = 20,
    val staffPageSize: Int = 20,
    val assignmentPageSize: Int = 20,
    val studentCursor: String? = null,
    val staffCursor: String? = null,
    val assignmentCursor: String? = null,
    val canLoadMoreStudents: Boolean = false,
    val canLoadMoreStaff: Boolean = false,
    val canLoadMoreAssignments: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class CampusAdminViewModel(
    private val repository: CampusRepository = CampusRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(CampusAdminUiState(loading = true))
    val uiState: StateFlow<CampusAdminUiState> = _uiState.asStateFlow()

    private var studentFilter = StudentFilter()
    private var staffFilter = StaffFilter()
    private var assignmentFilter = AssignmentFilter()

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, errorMessage = null, successMessage = null)
            try {
                val institutes = repository.fetchInstitutes()
                val selectedInstitute = institutes.firstOrNull()
                val instituteId = _uiState.value.selectedInstituteId.ifBlank { selectedInstitute?.id.orEmpty() }
                val instituteName = institutes.firstOrNull { it.id == instituteId }?.name ?: selectedInstitute?.name.orEmpty()

                studentFilter = studentFilter.copy(instituteId = instituteId)
                staffFilter = staffFilter.copy(instituteId = instituteId)
                assignmentFilter = assignmentFilter.copy(instituteId = instituteId)

                val studentsPage = repository.fetchStudentsPage(studentFilter, _uiState.value.studentPageSize.toLong())
                val staffPage = repository.fetchStaffPage(staffFilter, _uiState.value.staffPageSize.toLong())
                val assignmentPage = repository.fetchAssignmentPage(assignmentFilter, _uiState.value.assignmentPageSize.toLong())
                val classes = repository.fetchClasses(instituteId)
                val subjects = repository.fetchSubjects(instituteId)
                val branchOptions = repository.fetchBranchOptions(instituteId)
                val departmentOptions = repository.fetchDepartmentOptions(instituteId)

                _uiState.value = _uiState.value.copy(
                    loading = false,
                    institutes = institutes,
                    selectedInstituteId = instituteId,
                    selectedInstituteName = instituteName,
                    students = studentsPage.items,
                    staff = staffPage.items,
                    assignments = assignmentPage.items,
                    classes = classes,
                    subjects = subjects,
                    branchOptions = branchOptions,
                    departmentOptions = departmentOptions,
                    studentCursor = studentsPage.lastVisibleId,
                    staffCursor = staffPage.lastVisibleId,
                    assignmentCursor = assignmentPage.lastVisibleId,
                    canLoadMoreStudents = studentsPage.hasMore,
                    canLoadMoreStaff = staffPage.hasMore,
                    canLoadMoreAssignments = assignmentPage.hasMore
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    errorMessage = error.localizedMessage ?: "Unable to load campus data"
                )
            }
        }
    }

    fun selectInstitute(instituteId: String) {
        val instituteName = _uiState.value.institutes.firstOrNull { it.id == instituteId }?.name.orEmpty()
        _uiState.value = _uiState.value.copy(selectedInstituteId = instituteId, selectedInstituteName = instituteName)
        studentFilter = studentFilter.copy(instituteId = instituteId)
        staffFilter = staffFilter.copy(instituteId = instituteId)
        assignmentFilter = assignmentFilter.copy(instituteId = instituteId)
        refreshAll()
    }

    fun updateStudentFilter(filter: StudentFilter) {
        studentFilter = filter.copy(instituteId = _uiState.value.selectedInstituteId)
        reloadStudents()
    }

    fun updateStaffFilter(filter: StaffFilter) {
        staffFilter = filter.copy(instituteId = _uiState.value.selectedInstituteId)
        reloadStaff()
    }

    fun updateAssignmentFilter(filter: AssignmentFilter) {
        assignmentFilter = filter.copy(instituteId = _uiState.value.selectedInstituteId)
        reloadAssignments()
    }

    fun updatePageSize(size: Int) {
        _uiState.value = _uiState.value.copy(studentPageSize = size, staffPageSize = size, assignmentPageSize = size)
        refreshAll()
    }

    fun saveInstitute(institute: CampusInstitute) = executeSave("Institute saved") {
        repository.saveInstitute(institute)
        refreshAll()
    }

    fun deleteInstitute(id: String) = executeSave("Institute deleted") {
        repository.deleteInstitute(id)
        refreshAll()
    }

    fun saveStudent(student: StudentRecord) = executeSave("Student saved") {
        repository.saveStudent(student.copy(instituteId = _uiState.value.selectedInstituteId, instituteName = _uiState.value.selectedInstituteName))
        refreshAll()
    }

    fun deleteStudent(id: String) = executeSave("Student deleted") {
        repository.deleteStudent(id)
        reloadStudents()
    }

    fun saveStaff(staff: StaffRecord) = executeSave("Staff saved") {
        repository.saveStaff(staff.copy(instituteId = _uiState.value.selectedInstituteId, instituteName = _uiState.value.selectedInstituteName))
        refreshAll()
    }

    fun deleteStaff(id: String) = executeSave("Staff deleted") {
        repository.deleteStaff(id)
        reloadStaff()
    }

    fun saveTeacherAssignment(assignment: TeacherAssignment) = executeSave("Teacher assignment saved") {
        repository.saveTeacherAssignment(assignment.copy(instituteId = _uiState.value.selectedInstituteId, instituteName = _uiState.value.selectedInstituteName))
        refreshAll()
    }

    fun deleteTeacherAssignment(id: String) = executeSave("Teacher assignment deleted") {
        repository.deleteTeacherAssignment(id)
        reloadAssignments()
    }

    fun saveClassRecord(campusClass: CampusClass) = executeSave("Class saved") {
        repository.saveClassRecord(campusClass.copy(instituteId = _uiState.value.selectedInstituteId, instituteName = _uiState.value.selectedInstituteName))
        reloadAcademics()
    }

    fun deleteClassRecord(id: String) = executeSave("Class deleted") {
        repository.deleteClassRecord(id)
        reloadAcademics()
    }

    fun saveSubjectRecord(subject: SubjectRecord) = executeSave("Subject saved") {
        repository.saveSubjectRecord(subject.copy(instituteId = _uiState.value.selectedInstituteId))
        reloadAcademics()
    }

    fun deleteSubjectRecord(id: String) = executeSave("Subject deleted") {
        repository.deleteSubjectRecord(id)
        reloadAcademics()
    }

    fun importStudents(csvText: String) = executeImport("Students imported") {
        parseCsv(csvText).forEach { row ->
            repository.saveStudent(
                StudentRecord(
                    id = row["id"].orEmpty(),
                    userId = row["userId"].orEmpty().ifBlank { row["enrollmentNumber"].orEmpty() },
                    instituteId = _uiState.value.selectedInstituteId,
                    instituteName = _uiState.value.selectedInstituteName,
                    fullName = row["fullName"].orEmpty(),
                    enrollmentNumber = row["enrollmentNumber"].orEmpty(),
                    branch = row["branch"].orEmpty(),
                    semester = row["semester"].orEmpty().toIntOrNull() ?: 0,
                    division = row["division"].orEmpty(),
                    email = row["email"].orEmpty()
                )
            )
        }
        refreshAll()
    }

    fun importStaff(csvText: String) = executeImport("Staff imported") {
        parseCsv(csvText).forEach { row ->
            repository.saveStaff(
                StaffRecord(
                    id = row["id"].orEmpty(),
                    userId = row["userId"].orEmpty().ifBlank { row["employeeId"].orEmpty() },
                    instituteId = _uiState.value.selectedInstituteId,
                    instituteName = _uiState.value.selectedInstituteName,
                    fullName = row["fullName"].orEmpty(),
                    email = row["email"].orEmpty(),
                    employeeId = row["employeeId"].orEmpty(),
                    role = row["role"].orEmpty().ifBlank { CampusRoles.TEACHER },
                    department = row["department"].orEmpty(),
                    subjects = row["subjects"].orEmpty().split("|").map { it.trim() }.filter { it.isNotBlank() }
                )
            )
        }
        refreshAll()
    }

    fun importClasses(csvText: String) = executeImport("Classes imported") {
        parseCsv(csvText).forEach { row ->
            repository.saveClassRecord(
                CampusClass(
                    id = row["id"].orEmpty(),
                    instituteId = _uiState.value.selectedInstituteId,
                    instituteName = _uiState.value.selectedInstituteName,
                    branch = row["branch"].orEmpty(),
                    semester = row["semester"].orEmpty().toIntOrNull() ?: 0,
                    division = row["division"].orEmpty(),
                    className = row["className"].orEmpty()
                )
            )
        }
        refreshAll()
    }

    fun importSubjects(csvText: String) = executeImport("Subjects imported") {
        parseCsv(csvText).forEach { row ->
            repository.saveSubjectRecord(
                SubjectRecord(
                    id = row["id"].orEmpty(),
                    instituteId = _uiState.value.selectedInstituteId,
                    branch = row["branch"].orEmpty(),
                    semester = row["semester"].orEmpty().toIntOrNull() ?: 0,
                    code = row["code"].orEmpty(),
                    title = row["title"].orEmpty()
                )
            )
        }
        refreshAll()
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun loadMoreStudents() {
        viewModelScope.launch {
            if (!_uiState.value.canLoadMoreStudents) return@launch
            val page = repository.fetchStudentsPage(studentFilter, _uiState.value.studentPageSize.toLong(), _uiState.value.studentCursor)
            _uiState.value = _uiState.value.copy(
                students = _uiState.value.students + page.items,
                studentCursor = page.lastVisibleId,
                canLoadMoreStudents = page.hasMore
            )
        }
    }

    fun loadMoreStaff() {
        viewModelScope.launch {
            if (!_uiState.value.canLoadMoreStaff) return@launch
            val page = repository.fetchStaffPage(staffFilter, _uiState.value.staffPageSize.toLong(), _uiState.value.staffCursor)
            _uiState.value = _uiState.value.copy(
                staff = _uiState.value.staff + page.items,
                staffCursor = page.lastVisibleId,
                canLoadMoreStaff = page.hasMore
            )
        }
    }

    fun loadMoreAssignments() {
        viewModelScope.launch {
            if (!_uiState.value.canLoadMoreAssignments) return@launch
            val page = repository.fetchAssignmentPage(assignmentFilter, _uiState.value.assignmentPageSize.toLong(), _uiState.value.assignmentCursor)
            _uiState.value = _uiState.value.copy(
                assignments = _uiState.value.assignments + page.items,
                assignmentCursor = page.lastVisibleId,
                canLoadMoreAssignments = page.hasMore
            )
        }
    }

    private fun reloadStudents() {
        viewModelScope.launch {
            val page = repository.fetchStudentsPage(studentFilter, _uiState.value.studentPageSize.toLong())
            _uiState.value = _uiState.value.copy(
                students = page.items,
                studentCursor = page.lastVisibleId,
                canLoadMoreStudents = page.hasMore
            )
        }
    }

    private fun reloadStaff() {
        viewModelScope.launch {
            val page = repository.fetchStaffPage(staffFilter, _uiState.value.staffPageSize.toLong())
            _uiState.value = _uiState.value.copy(
                staff = page.items,
                staffCursor = page.lastVisibleId,
                canLoadMoreStaff = page.hasMore
            )
        }
    }

    private fun reloadAssignments() {
        viewModelScope.launch {
            val page = repository.fetchAssignmentPage(assignmentFilter, _uiState.value.assignmentPageSize.toLong())
            _uiState.value = _uiState.value.copy(
                assignments = page.items,
                assignmentCursor = page.lastVisibleId,
                canLoadMoreAssignments = page.hasMore
            )
        }
    }

    private fun reloadAcademics() {
        viewModelScope.launch {
            val instituteId = _uiState.value.selectedInstituteId
            _uiState.value = _uiState.value.copy(
                classes = repository.fetchClasses(instituteId),
                subjects = repository.fetchSubjects(instituteId),
                branchOptions = repository.fetchBranchOptions(instituteId),
                departmentOptions = repository.fetchDepartmentOptions(instituteId)
            )
        }
    }

    private fun executeSave(successMessage: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true, errorMessage = null, successMessage = null)
            try {
                action()
                _uiState.value = _uiState.value.copy(saving = false, successMessage = successMessage)
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(saving = false, errorMessage = error.localizedMessage ?: "Unable to save data")
            }
        }
    }

    private fun executeImport(successMessage: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(importing = true, errorMessage = null, successMessage = null)
            try {
                action()
                _uiState.value = _uiState.value.copy(importing = false, successMessage = successMessage)
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(importing = false, errorMessage = error.localizedMessage ?: "Unable to import data")
            }
        }
    }

    private fun parseCsv(csvText: String): List<Map<String, String>> {
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size < 2) return emptyList()
        val headers = lines.first().split(",").map { it.trim() }
        return lines.drop(1).map { line ->
            val values = line.split(",").map { it.trim() }
            headers.mapIndexed { index, header -> header to values.getOrElse(index) { "" } }.toMap()
        }
    }
}
