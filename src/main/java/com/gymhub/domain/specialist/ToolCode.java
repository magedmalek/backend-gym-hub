package com.gymhub.domain.specialist;

import java.util.EnumSet;
import java.util.Set;

public enum ToolCode {

    NUTRITION_PLAN_CREATION,
    NUTRITION_PLAN_EDITING,
    MEAL_LOG_REVIEW,
    FOOD_PREFERENCES,
    SUPPLEMENT_SCHEDULES,
    WATER_TRACKING_REVIEW,

    WORKOUT_PLAN_CREATION,
    WORKOUT_PLAN_EDITING,
    TRAINING_EXECUTION_REVIEW,
    TRAINING_EXECUTION_LOGGING,
    MEASUREMENTS,
    PROGRESS_PHOTOS,

    APPOINTMENTS,
    FOLLOW_UPS,
    CALENDAR,
    CLIENT_TIMELINE,
    ADVANCED_ANALYTICS;

    public static Set<ToolCode> forAxis(ServiceAxis axis) {
        return switch (axis) {
            case NUTRITION -> EnumSet.of(
                    NUTRITION_PLAN_CREATION, NUTRITION_PLAN_EDITING,
                    MEAL_LOG_REVIEW, FOOD_PREFERENCES, SUPPLEMENT_SCHEDULES, WATER_TRACKING_REVIEW);
            case WORKOUT_PLAN -> EnumSet.of(
                    WORKOUT_PLAN_CREATION, WORKOUT_PLAN_EDITING,
                    TRAINING_EXECUTION_REVIEW, MEASUREMENTS, PROGRESS_PHOTOS, CALENDAR, CLIENT_TIMELINE);
            case TRAINING_EXECUTION -> EnumSet.of(
                    TRAINING_EXECUTION_LOGGING, APPOINTMENTS, FOLLOW_UPS, CALENDAR, CLIENT_TIMELINE);
            case MIXED -> EnumSet.allOf(ToolCode.class);
        };
    }
}
