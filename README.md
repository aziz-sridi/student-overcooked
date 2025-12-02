# Student OverCooked 🔥

A multi-user mobile application that helps students manage academic and project workloads in a clear and organized way.

## Project Overview

Student OverCooked solves common problems students face:
- ✅ Many overlapping deadlines
- ✅ Group projects with poor coordination
- ✅ Scattered communication channels
- ✅ No clear sense of priority
- ✅ High stress without visibility

## Core Features

### 🎯 Cooked Meter
A local workload indicator expressed as a percentage (0–100%), based on:
- Number of pending tasks
- Deadline proximity
- Task priority weight

**Meter States:**
| Level | Range | Emoji | Description |
|-------|-------|-------|-------------|
| Cozy | 0-30% | 😌 | Low workload, student is relaxed |
| Crispy | 31-60% | ☕ | Moderate workload, some pressure |
| Cooked | 61-85% | 🔥 | High workload, significant stress |
| Overcooked | 86-100% | 💀 | Critical workload, maximum stress |

### 📝 Task Management
- Create, edit, and delete personal tasks
- Task attributes: title, deadline, subject/course, notes, priority
- Mark tasks as completed
- Filter tasks by: All, Today, Upcoming, Overdue, Completed
- Priority-based organization

### 📂 Project Management
- Create individual or team projects
- Track project progress with task completion
- Set project deadlines and courses
- Color-coded projects for easy identification

### 👥 Team Collaboration (Future)
- Join existing project groups
- View and update shared project tasks
- See group progress and task statuses
- Real-time group chat

## Tech Stack

### Mobile Application
- **IDE:** Android Studio
- **Language:** Kotlin & Java
- **UI:** XML-based layouts with Material Design 3
- **Architecture:** MVVM with Repository pattern

### Local Database (Room/SQLite)
Stores structured and offline-critical data:
- Personal tasks
- Projects
- Team members
- Cooked Meter calculations

### Cloud Database (Firebase - Ready to Enable)
Firebase Firestore for real-time collaborative data:
- Group information
- Shared tasks
- Group chat messages
- Member activity updates

### Authentication (Firebase - Ready to Enable)
Firebase Authentication (Email/Password or Google Sign-In)

## Project Structure

```
app/src/main/java/com/example/overcooked/
├── data/
│   ├── dao/                    # Data Access Objects
│   │   ├── TaskDao.kt
│   │   ├── ProjectDao.kt
│   │   └── TeamMemberDao.kt
│   ├── database/               # Room Database
│   │   ├── OvercookedDatabase.kt
│   │   └── Converters.kt
│   ├── model/                  # Data Models
│   │   ├── Task.kt
│   │   ├── Project.kt
│   │   ├── TeamMember.kt
│   │   ├── Priority.kt
│   │   ├── TaskType.kt
│   │   ├── CookedLevel.kt
│   │   └── User.kt
│   └── repository/             # Repository Layer
│       ├── TaskRepository.kt
│       └── ProjectRepository.kt
├── ui/                         # UI Layer
│   ├── adapter/                # RecyclerView Adapters
│   │   ├── TaskAdapter.kt
│   │   ├── ProjectAdapter.kt
│   │   ├── QuickStatsAdapter.kt
│   │   └── TeamMemberAdapter.kt
│   ├── HomeActivity.kt
│   ├── AddTaskActivityKotlin.kt
│   ├── AddProjectActivityKotlin.kt
│   ├── TasksListActivityKotlin.kt
│   └── ProjectDetailsActivity.java
├── util/                       # Utilities
│   └── CookedMeterCalculator.kt
├── MainActivity.kt             # Entry point (Splash)
└── OvercookedApplication.kt    # Application class
```

## Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 21
- Android SDK 24+ (minSdk)

### Setup

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Run on emulator or device

### Enable Firebase (Optional)
1. Create a Firebase project
2. Add `google-services.json` to the `app/` directory
3. Uncomment Firebase dependencies in `app/build.gradle.kts`
4. Uncomment `google-services` plugin in `app/build.gradle.kts`

## Dependencies

- **Room Database** - Local SQLite storage
- **Coroutines** - Asynchronous programming
- **Material Design 3** - Modern UI components
- **Lifecycle & ViewModel** - Architecture components
- **Firebase (optional)** - Auth & Firestore

## Actors

### Student (Primary User)
- Manages personal tasks
- Monitors workload through Cooked Meter
- Receives deadline reminders

### Group Member
- Participates in project groups
- Views and updates shared tasks
- Sees group progress

### Group Admin
- Creates project groups
- Manages team members
- Edits shared tasks

## Future Enhancements

- [ ] Google Classroom integration
- [ ] Push notifications for deadlines
- [ ] Calendar view
- [ ] Task statistics and analytics
- [ ] Export/Import tasks
- [ ] Dark mode support
- [ ] Widget for quick task view

## License

This project is for educational purposes.

---

Built with 🔥 by students, for students.
