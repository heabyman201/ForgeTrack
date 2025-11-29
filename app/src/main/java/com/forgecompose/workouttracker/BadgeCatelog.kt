package com.forgecompose.workouttracker

object BadgeCatalog {

    val totalWorkoutBadges = listOf(
        BadgeDefinition(
            id = "total_10",
            title = "Getting Started",
            description = "Log 10 workouts.",
            category = BadgeCategory.TOTAL_WORKOUTS,
            target = 10
        ),
        BadgeDefinition(
            id = "total_50",
            title = "Consistency Rookie",
            description = "Log 50 workouts.",
            category = BadgeCategory.TOTAL_WORKOUTS,
            target = 50
        ),
        BadgeDefinition(
            id = "total_100",
            title = "Workout Machine",
            description = "Log 100 workouts.",
            category = BadgeCategory.TOTAL_WORKOUTS,
            target = 100
        ),
        BadgeDefinition(
            id = "total_150",
            title = "Advanced Consistency",
            description = "Log 150 workouts.",
            category = BadgeCategory.TOTAL_WORKOUTS,
            target = 150
        ),
        BadgeDefinition(
            id = "total_200",
            title = "Long-Term Commitment",
            description = "Log 200 workouts.",
            category = BadgeCategory.TOTAL_WORKOUTS,
            target = 200
        ),
        BadgeDefinition(
            id = "total_300",
            title = "Sustained Performance",
            description = "Log 300 workouts.",
            category = BadgeCategory.TOTAL_WORKOUTS,
            target = 300
        ),
        BadgeDefinition(
            id = "total_500",
            title = "High Consistency",
            description = "Log 500 workouts.",
            category = BadgeCategory.TOTAL_WORKOUTS,
            target = 500
        ),
        BadgeDefinition(
            id = "total_750",
            title = "Elite Commitment",
            description = "Log 750 workouts.",
            category = BadgeCategory.TOTAL_WORKOUTS,
            target = 750
        ),
        BadgeDefinition(
            id = "total_1000",
            title = "One Thousand Workouts",
            description = "Log 1000 workouts.",
            category = BadgeCategory.TOTAL_WORKOUTS,
            target = 1000
        ),
        BadgeDefinition(
            id = "total_1500",
            title = "Forged in Steel",
            description = "Log 1500 workouts and maintain long-term consistency.",
            category = BadgeCategory.TOTAL_WORKOUTS,
            target = 1500
        ),

        BadgeDefinition(
            id = "total_2000",
            title = "Heat-Treated Endurance",
            description = "Log 2000 total workouts.",
            category = BadgeCategory.TOTAL_WORKOUTS,
            target = 2000
        ),

        BadgeDefinition(
            id = "total_2500",
            title = "Cold-Forged Discipline",
            description = "Reach 2500 workouts with unwavering focus.",
            category = BadgeCategory.TOTAL_WORKOUTS,
            target = 2500
        ),

        BadgeDefinition(
            id = "total_3000",
            title = "Tempered Strength",
            description = "Log 3000 workouts with sustained effort.",
            category = BadgeCategory.TOTAL_WORKOUTS,
            target = 3000
        ),

        BadgeDefinition(
            id = "total_4000",
            title = "Pressure-Hardened",
            description = "Achieve 4000 total logged workouts.",
            category = BadgeCategory.TOTAL_WORKOUTS,
            target = 4000
        ),

        BadgeDefinition(
            id = "total_5000",
            title = "Full Metal Consistency",
            description = "Reach 5000 workouts with exceptional discipline.",
            category = BadgeCategory.TOTAL_WORKOUTS,
            target = 5000
        ),

        BadgeDefinition(
            id = "total_7500",
            title = "Iron Temperament",
            description = "Log 7500 workouts with long-term adherence.",
            category = BadgeCategory.TOTAL_WORKOUTS,
            target = 7500
        ),

        BadgeDefinition(
            id = "total_8000",
            title = "Master of the Forge",
            description = "Reach a lifetime milestone of 8,000 workouts.",
            category = BadgeCategory.TOTAL_WORKOUTS,
            target = 8000
        )

    )

    private fun muscleBadges(
        muscle: MuscleId,
        baseId: String,
        label: String
    ): List<BadgeDefinition> = listOf(
        BadgeDefinition(
            id = "${baseId}_10",
            title = "$label Beginner",
            description = "Train $label in 10 workouts.",
            category = BadgeCategory.MUSCLE,
            target = 10,
            muscleId = muscle
        ),
        BadgeDefinition(
            id = "${baseId}_25",
            title = "$label Grinder",
            description = "Train $label in 25 workouts.",
            category = BadgeCategory.MUSCLE,
            target = 25,
            muscleId = muscle
        ),
        BadgeDefinition(
            id = "${baseId}_50",
            title = "$label Demon",
            description = "Train $label in 50 workouts.",
            category = BadgeCategory.MUSCLE,
            target = 50,
            muscleId = muscle
        )
    )

    val muscleBadges: List<BadgeDefinition> = buildList {
        addAll(muscleBadges(MuscleId.CHEST, "chest", "Chest"))
        addAll(muscleBadges(MuscleId.BACK, "back", "Back"))
        addAll(muscleBadges(MuscleId.SHOULDERS, "shoulders", "Shoulders"))
        addAll(muscleBadges(MuscleId.LEGS, "legs", "Legs"))
        addAll(muscleBadges(MuscleId.ARMS, "arms", "Arms"))
        addAll(muscleBadges(MuscleId.CORE, "core", "Core"))
    }

    val allBadges: List<BadgeDefinition> =
        totalWorkoutBadges + muscleBadges
}
