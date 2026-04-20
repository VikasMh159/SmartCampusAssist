package com.smartcampusassist.campus

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class CampusRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun fetchInstitutes(): List<CampusInstitute> {
        return firestore.collection(CampusCollections.INSTITUTES)
            .orderBy("name")
            .get()
            .await()
            .toObjects(CampusInstitute::class.java)
    }

    suspend fun saveInstitute(institute: CampusInstitute) {
        val documentId = institute.id.ifBlank { firestore.collection(CampusCollections.INSTITUTES).document().id }
        val now = System.currentTimeMillis()
        firestore.collection(CampusCollections.INSTITUTES)
            .document(documentId)
            .set(
                mapOf(
                    "name" to institute.name.trim(),
                    "code" to institute.code.trim().uppercase(),
                    "type" to institute.type.trim(),
                    "status" to institute.status,
                    "studentCount" to institute.studentCount,
                    "staffCount" to institute.staffCount,
                    "createdAt" to if (institute.createdAt > 0L) institute.createdAt else now,
                    "updatedAt" to now
                )
            )
            .await()
    }

    suspend fun saveStudent(student: StudentRecord) {
        val documentId = student.id.ifBlank { student.userId.ifBlank { firestore.collection(CampusCollections.STUDENTS).document().id } }
        val now = System.currentTimeMillis()
        firestore.collection(CampusCollections.STUDENTS)
            .document(documentId)
            .set(
                mapOf(
                    "userId" to student.userId.ifBlank { documentId },
                    "instituteId" to student.instituteId.trim(),
                    "instituteName" to student.instituteName.trim(),
                    "fullName" to student.fullName.trim(),
                    "searchableName" to student.fullName.toSearchToken(),
                    "enrollmentNumber" to student.enrollmentNumber.trim().uppercase(),
                    "branch" to student.branch.trim(),
                    "semester" to student.semester,
                    "division" to student.division.trim().uppercase(),
                    "email" to student.email.trim().lowercase(),
                    "role" to CampusRoles.STUDENT,
                    "status" to student.status,
                    "createdAt" to if (student.createdAt > 0L) student.createdAt else now,
                    "updatedAt" to now
                )
            )
            .await()

        syncCampusUser(
            userId = student.userId.ifBlank { documentId },
            email = student.email,
            fullName = student.fullName,
            role = CampusRoles.STUDENT,
            instituteId = student.instituteId,
            instituteName = student.instituteName,
            branch = student.branch,
            semester = student.semester,
            division = student.division,
            enrollmentNumber = student.enrollmentNumber
        )
    }

    suspend fun saveStaff(staff: StaffRecord) {
        val documentId = staff.id.ifBlank { staff.userId.ifBlank { firestore.collection(CampusCollections.STAFF).document().id } }
        val now = System.currentTimeMillis()
        firestore.collection(CampusCollections.STAFF)
            .document(documentId)
            .set(
                mapOf(
                    "userId" to staff.userId.ifBlank { documentId },
                    "instituteId" to staff.instituteId.trim(),
                    "instituteName" to staff.instituteName.trim(),
                    "fullName" to staff.fullName.trim(),
                    "searchableName" to staff.fullName.toSearchToken(),
                    "email" to staff.email.trim().lowercase(),
                    "employeeId" to staff.employeeId.trim().uppercase(),
                    "role" to staff.role.trim().ifBlank { CampusRoles.TEACHER },
                    "department" to staff.department.trim(),
                    "subjects" to staff.subjects.map { it.trim() }.filter { it.isNotBlank() },
                    "status" to staff.status,
                    "createdAt" to if (staff.createdAt > 0L) staff.createdAt else now,
                    "updatedAt" to now
                )
            )
            .await()

        syncCampusUser(
            userId = staff.userId.ifBlank { documentId },
            email = staff.email,
            fullName = staff.fullName,
            role = staff.role,
            instituteId = staff.instituteId,
            instituteName = staff.instituteName,
            department = staff.department,
            employeeId = staff.employeeId
        )
    }

    suspend fun saveTeacherAssignment(assignment: TeacherAssignment) {
        val documentId = assignment.id.ifBlank { firestore.collection(CampusCollections.TEACHER_ASSIGNMENTS).document().id }
        val now = System.currentTimeMillis()
        firestore.collection(CampusCollections.TEACHER_ASSIGNMENTS)
            .document(documentId)
            .set(
                mapOf(
                    "instituteId" to assignment.instituteId.trim(),
                    "instituteName" to assignment.instituteName.trim(),
                    "teacherId" to assignment.teacherId.trim(),
                    "teacherName" to assignment.teacherName.trim(),
                    "searchableTeacherName" to assignment.teacherName.toSearchToken(),
                    "staffRole" to assignment.staffRole.trim().ifBlank { CampusRoles.TEACHER },
                    "classId" to assignment.classId.trim(),
                    "className" to assignment.className.trim(),
                    "subjectId" to assignment.subjectId.trim(),
                    "subjectCode" to assignment.subjectCode.trim().uppercase(),
                    "subjectTitle" to assignment.subjectTitle.trim(),
                    "semester" to assignment.semester,
                    "division" to assignment.division.trim().uppercase(),
                    "branch" to assignment.branch.trim(),
                    "status" to assignment.status,
                    "createdAt" to if (assignment.createdAt > 0L) assignment.createdAt else now,
                    "updatedAt" to now
                )
            )
            .await()
    }

    suspend fun saveClassRecord(campusClass: CampusClass) {
        val documentId = campusClass.id.ifBlank { firestore.collection(CampusCollections.CLASSES).document().id }
        val now = System.currentTimeMillis()
        firestore.collection(CampusCollections.CLASSES)
            .document(documentId)
            .set(
                mapOf(
                    "instituteId" to campusClass.instituteId.trim(),
                    "instituteName" to campusClass.instituteName.trim(),
                    "branch" to campusClass.branch.trim(),
                    "semester" to campusClass.semester,
                    "division" to campusClass.division.trim().uppercase(),
                    "className" to campusClass.className.trim(),
                    "searchableName" to campusClass.className.toSearchToken(),
                    "status" to campusClass.status,
                    "createdAt" to if (campusClass.createdAt > 0L) campusClass.createdAt else now,
                    "updatedAt" to now
                )
            )
            .await()
    }

    suspend fun saveSubjectRecord(subject: SubjectRecord) {
        val documentId = subject.id.ifBlank { firestore.collection(CampusCollections.SUBJECTS).document().id }
        val now = System.currentTimeMillis()
        firestore.collection(CampusCollections.SUBJECTS)
            .document(documentId)
            .set(
                mapOf(
                    "instituteId" to subject.instituteId.trim(),
                    "branch" to subject.branch.trim(),
                    "semester" to subject.semester,
                    "code" to subject.code.trim().uppercase(),
                    "title" to subject.title.trim(),
                    "searchableTitle" to subject.title.toSearchToken(),
                    "status" to subject.status,
                    "createdAt" to if (subject.createdAt > 0L) subject.createdAt else now,
                    "updatedAt" to now
                )
            )
            .await()
    }

    suspend fun fetchBranchOptions(instituteId: String): List<String> {
        if (instituteId.isBlank()) return emptyList()

        val classesSnapshot = firestore.collection(CampusCollections.CLASSES)
            .whereEqualTo("instituteId", instituteId.trim())
            .get()
            .await()

        val subjectsSnapshot = firestore.collection(CampusCollections.SUBJECTS)
            .whereEqualTo("instituteId", instituteId.trim())
            .get()
            .await()

        val studentsSnapshot = firestore.collection(CampusCollections.STUDENTS)
            .whereEqualTo("instituteId", instituteId.trim())
            .get()
            .await()

        return buildSet {
            classesSnapshot.documents.mapNotNullTo(this) { it.getString("branch")?.trim()?.takeIf(String::isNotBlank) }
            subjectsSnapshot.documents.mapNotNullTo(this) { it.getString("branch")?.trim()?.takeIf(String::isNotBlank) }
            studentsSnapshot.documents.mapNotNullTo(this) { it.getString("branch")?.trim()?.takeIf(String::isNotBlank) }
        }.sorted()
    }

    suspend fun fetchDepartmentOptions(instituteId: String): List<String> {
        if (instituteId.isBlank()) return emptyList()

        val staffSnapshot = firestore.collection(CampusCollections.STAFF)
            .whereEqualTo("instituteId", instituteId.trim())
            .get()
            .await()

        val userSnapshot = firestore.collection(CampusCollections.USERS)
            .whereEqualTo("instituteId", instituteId.trim())
            .get()
            .await()

        return buildSet {
            staffSnapshot.documents.mapNotNullTo(this) { it.getString("department")?.trim()?.takeIf(String::isNotBlank) }
            userSnapshot.documents.mapNotNullTo(this) { it.getString("department")?.trim()?.takeIf(String::isNotBlank) }
        }.sorted()
    }

    suspend fun fetchClasses(instituteId: String): List<CampusClass> {
        if (instituteId.isBlank()) return emptyList()
        return firestore.collection(CampusCollections.CLASSES)
            .whereEqualTo("instituteId", instituteId.trim())
            .orderBy("searchableName")
            .get()
            .await()
            .toObjects(CampusClass::class.java)
    }

    suspend fun fetchSubjects(instituteId: String): List<SubjectRecord> {
        if (instituteId.isBlank()) return emptyList()
        return firestore.collection(CampusCollections.SUBJECTS)
            .whereEqualTo("instituteId", instituteId.trim())
            .orderBy("searchableTitle")
            .get()
            .await()
            .toObjects(SubjectRecord::class.java)
    }

    suspend fun deleteInstitute(id: String) {
        deleteDocument(CampusCollections.INSTITUTES, id)
    }

    suspend fun deleteStudent(id: String) {
        deleteDocument(CampusCollections.STUDENTS, id)
    }

    suspend fun deleteStaff(id: String) {
        deleteDocument(CampusCollections.STAFF, id)
    }

    suspend fun deleteTeacherAssignment(id: String) {
        deleteDocument(CampusCollections.TEACHER_ASSIGNMENTS, id)
    }

    suspend fun deleteClassRecord(id: String) {
        deleteDocument(CampusCollections.CLASSES, id)
    }

    suspend fun deleteSubjectRecord(id: String) {
        deleteDocument(CampusCollections.SUBJECTS, id)
    }

    suspend fun fetchStudentsPage(
        filter: StudentFilter,
        pageSize: Long = 20,
        lastVisibleId: String? = null
    ): PageResult<StudentRecord> {
        val pageQuery = buildStudentQuery(filter)
            .let { query ->
                val lastDocument = resolveLastDocument(CampusCollections.STUDENTS, lastVisibleId)
                if (lastDocument != null) query.startAfter(lastDocument) else query
            }
            .limit(pageSize)

        val snapshot = pageQuery.get().await()
        val items = snapshot.toObjects(StudentRecord::class.java)
        return PageResult(
            items = items,
            lastVisibleId = snapshot.documents.lastOrNull()?.id,
            hasMore = items.size >= pageSize
        )
    }

    suspend fun fetchStaffPage(
        filter: StaffFilter,
        pageSize: Long = 20,
        lastVisibleId: String? = null
    ): PageResult<StaffRecord> {
        val pageQuery = buildStaffQuery(filter)
            .let { query ->
                val lastDocument = resolveLastDocument(CampusCollections.STAFF, lastVisibleId)
                if (lastDocument != null) query.startAfter(lastDocument) else query
            }
            .limit(pageSize)

        val snapshot = pageQuery.get().await()
        val items = snapshot.toObjects(StaffRecord::class.java)
        return PageResult(
            items = items,
            lastVisibleId = snapshot.documents.lastOrNull()?.id,
            hasMore = items.size >= pageSize
        )
    }

    suspend fun fetchAssignmentPage(
        filter: AssignmentFilter,
        pageSize: Long = 20,
        lastVisibleId: String? = null
    ): PageResult<TeacherAssignment> {
        val pageQuery = buildAssignmentQuery(filter)
            .let { query ->
                val lastDocument = resolveLastDocument(CampusCollections.TEACHER_ASSIGNMENTS, lastVisibleId)
                if (lastDocument != null) query.startAfter(lastDocument) else query
            }
            .limit(pageSize)

        val snapshot = pageQuery.get().await()
        val items = snapshot.toObjects(TeacherAssignment::class.java)
        return PageResult(
            items = items,
            lastVisibleId = snapshot.documents.lastOrNull()?.id,
            hasMore = items.size >= pageSize
        )
    }

    private suspend fun syncCampusUser(
        userId: String,
        email: String,
        fullName: String,
        role: String,
        instituteId: String,
        instituteName: String,
        department: String = "",
        branch: String = "",
        semester: Int = 0,
        division: String = "",
        enrollmentNumber: String = "",
        employeeId: String = ""
    ) {
        val now = System.currentTimeMillis()
        firestore.collection(CampusCollections.USERS)
            .document(userId)
            .set(
                mapOf(
                    "uid" to userId,
                    "email" to email.trim().lowercase(),
                    "fullName" to fullName.trim(),
                    "role" to role.trim(),
                    "instituteId" to instituteId.trim(),
                    "instituteName" to instituteName.trim(),
                    "department" to department.trim(),
                    "branch" to branch.trim(),
                    "semester" to semester,
                    "division" to division.trim().uppercase(),
                    "enrollmentNumber" to enrollmentNumber.trim().uppercase(),
                    "employeeId" to employeeId.trim().uppercase(),
                    "searchableName" to fullName.toSearchToken(),
                    "status" to "active",
                    "createdAt" to now,
                    "updatedAt" to now
                )
            )
            .await()
    }

    private suspend fun resolveLastDocument(
        collection: String,
        lastVisibleId: String?
    ): DocumentSnapshot? {
        if (lastVisibleId.isNullOrBlank()) return null
        val document = firestore.collection(collection).document(lastVisibleId).get().await()
        return document.takeIf { it.exists() }
    }

    private suspend fun deleteDocument(collection: String, id: String) {
        if (id.isBlank()) return
        firestore.collection(collection)
            .document(id)
            .delete()
            .await()
    }

    private fun buildStudentQuery(filter: StudentFilter): Query {
        var query: Query = firestore.collection(CampusCollections.STUDENTS)

        if (filter.instituteId.isNotBlank()) {
            query = query.whereEqualTo("instituteId", filter.instituteId.trim())
        }
        if (filter.enrollmentNumber.isNotBlank()) {
            query = query.whereEqualTo("enrollmentNumber", filter.enrollmentNumber.trim().uppercase())
        }
        if (filter.branch.isNotBlank()) {
            query = query.whereEqualTo("branch", filter.branch.trim())
        }
        if (filter.semester != null) {
            query = query.whereEqualTo("semester", filter.semester)
        }
        if (filter.division.isNotBlank()) {
            query = query.whereEqualTo("division", filter.division.trim().uppercase())
        }
        if (filter.query.isNotBlank()) {
            val normalized = filter.query.toSearchToken()
            query = query
                .orderBy("searchableName")
                .startAt(normalized)
                .endAt(normalized + "\uf8ff")
        } else {
            query = query.orderBy("searchableName")
        }
        return query
    }

    private fun buildStaffQuery(filter: StaffFilter): Query {
        var query: Query = firestore.collection(CampusCollections.STAFF)
        if (filter.instituteId.isNotBlank()) {
            query = query.whereEqualTo("instituteId", filter.instituteId.trim())
        }
        if (filter.role.isNotBlank()) {
            query = query.whereEqualTo("role", filter.role.trim())
        }
        if (filter.department.isNotBlank()) {
            query = query.whereEqualTo("department", filter.department.trim())
        }
        if (filter.query.isNotBlank()) {
            val normalized = filter.query.toSearchToken()
            query = query
                .orderBy("searchableName")
                .startAt(normalized)
                .endAt(normalized + "\uf8ff")
        } else {
            query = query.orderBy("searchableName")
        }
        return query
    }

    private fun buildAssignmentQuery(filter: AssignmentFilter): Query {
        var query: Query = firestore.collection(CampusCollections.TEACHER_ASSIGNMENTS)
        if (filter.instituteId.isNotBlank()) {
            query = query.whereEqualTo("instituteId", filter.instituteId.trim())
        }
        if (filter.role.isNotBlank()) {
            query = query.whereEqualTo("staffRole", filter.role.trim())
        }
        if (filter.branch.isNotBlank()) {
            query = query.whereEqualTo("branch", filter.branch.trim())
        }
        if (filter.semester != null) {
            query = query.whereEqualTo("semester", filter.semester)
        }
        if (filter.teacherName.isNotBlank()) {
            val normalized = filter.teacherName.toSearchToken()
            query = query
                .orderBy("searchableTeacherName")
                .startAt(normalized)
                .endAt(normalized + "\uf8ff")
        } else {
            query = query.orderBy("searchableTeacherName")
        }
        return query
    }
}
