package com.smartcampusassist.jpui.assistant

import com.google.ai.client.generativeai.GenerativeModel
import com.smartcampusassist.BuildConfig

object GeminiHelper {

    private val campusKeywords = listOf(
        "campus", "college", "school", "university", "class", "classes", "lecture", "lectures",
        "schedule", "timetable", "assignment", "assignments", "homework", "exam", "exams",
        "test", "tests", "quiz", "quizzes", "subject", "subjects", "teacher", "teachers",
        "faculty", "professor", "professors", "student", "students", "attendance", "syllabus",
        "semester", "result", "results", "grade", "grades", "cgpa", "gpa", "lab", "labs",
        "practical", "practicals", "library", "hostel", "canteen", "bus", "transport",
        "notice", "notices", "reminder", "reminders", "event", "events", "club", "clubs",
        "department", "admission", "admissions", "scholarship", "scholarships", "fees",
        "fee", "course", "courses", "study", "studies", "study plan", "classroom",
        "sir", "maam", "madam", "hod", "camp", "semester exam", "internal", "external",
        "placement", "placements", "internship", "internships", "project", "projects"
    )
    private val campusIntentKeywords = listOf(
        "when", "where", "what", "how", "today", "tomorrow", "upcoming", "next", "due", "date",
        "room", "faculty", "teacher", "subject", "semester", "department", "schedule", "assignment",
        "event", "reminder", "exam", "attendance", "class", "campus"
    )

    private const val offTopicReply =
        "I can only help with campus-related questions. Ask about classes, schedules, assignments, exams, events, reminders, faculty, facilities, or study help."

    suspend fun askGemini(
        question: String,
        groundedContext: String = "",
        userRole: String = "student"
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()

        if (apiKey.isBlank()) {
            return buildOfflineFallback(question, groundedContext, userRole)
        }

        if (!isCampusRelated(question, groundedContext)) {
            return offTopicReply
        }

        return try {
            val model = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = apiKey
            )

            val response = model.generateContent(
                """
                You are Smart Campus Assist, a campus assistant.
                Answer the user's message clearly, naturally, and conversationally.
                Keep the response well-structured, concise, and easy to read.
                Avoid messy formatting, raw JSON, stack traces, or unnecessary symbols.
                Reply in clear English.
                Keep the tone natural and helpful.
                Use short paragraphs with proper spacing between them.
                When needed, separate points cleanly instead of merging everything into one block.
                You must only answer campus-related topics such as schedules, assignments, classes, reminders, events, faculty, facilities, exams, and study support.
                If the message is off-topic, refuse briefly and ask the user to send a campus-related question only.
                You must ground your answer in the campus data context below whenever relevant.
                If the answer is not supported by the context, clearly say the data is not available instead of inventing details.
                Tailor the answer for the user's role: $userRole.

                Campus data context:
                $groundedContext

                User question:
                $question
                """.trimIndent()
            )

            cleanResponse(response.text)

        } catch (e: Exception) {
            buildOfflineFallback(question, groundedContext, userRole)
        }
    }

    private fun cleanResponse(text: String?): String {
        val cleaned = text
            ?.replace(Regex("\\r\\n"), "\n")
            ?.replace(Regex("\\n{3,}"), "\n\n")
            ?.replace(Regex("[ \\t]{2,}"), " ")
            ?.replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            ?.replace(Regex("\\*(.*?)\\*"), "$1")
            ?.replace(Regex("^[\\-*•]\\s+", RegexOption.MULTILINE), "")
            ?.trim()

        return cleaned?.ifBlank { null } ?: "Sorry, I couldn't understand."
    }

    private fun isCampusRelated(question: String, groundedContext: String): Boolean {
        val normalizedQuestion = question.lowercase()
        return campusKeywords.any { keyword -> normalizedQuestion.contains(keyword) } ||
            campusIntentKeywords.any { keyword -> normalizedQuestion.contains(keyword) } ||
            groundedContext.lowercase().contains(normalizedQuestion.take(20))
    }

    private fun buildOfflineFallback(
        question: String,
        groundedContext: String,
        userRole: String
    ): String {
        val normalizedQuestion = question.lowercase()
        val rolePrefix = if (userRole.equals("teacher", ignoreCase = true)) {
            "Teacher view"
        } else {
            "Student view"
        }

        val topic = when {
            "assignment" in normalizedQuestion || "homework" in normalizedQuestion -> "assignments"
            "schedule" in normalizedQuestion || "class" in normalizedQuestion || "timetable" in normalizedQuestion -> "schedule"
            "event" in normalizedQuestion -> "events"
            "reminder" in normalizedQuestion || "notice" in normalizedQuestion -> "reminders"
            "exam" in normalizedQuestion || "quiz" in normalizedQuestion || "test" in normalizedQuestion -> "exams"
            else -> "campus details"
        }

        val contextHint = groundedContext
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { line ->
                line.isNotBlank() && (
                    topic in line.lowercase() ||
                        normalizedQuestion.split(" ").any { token -> token.length > 3 && token in line.lowercase() }
                    )
            }

        return if (contextHint != null) {
            "$rolePrefix: I found a related detail in your saved campus data.\n\n$contextHint"
        } else {
            "$rolePrefix: I could not reach live AI right now, but you can still ask about $topic. Try a shorter question like 'Show my $topic' or 'What is next in my $topic?'."
        }
    }
}
