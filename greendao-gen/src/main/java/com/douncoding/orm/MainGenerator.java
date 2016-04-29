package com.douncoding.orm;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Property;
import de.greenrobot.daogenerator.Schema;

public class MainGenerator {
    public static void main(String[] args) throws Exception {
        Schema schema = new Schema(2, "com.douncoding.dao");
        schema.enableActiveEntitiesByDefault();

        addTables(schema);

        try {
            new DaoGenerator().generateAll(schema, "../app/src/main/java");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addTables(final Schema schema) {
        Entity place = addPlace(schema);
        Entity instructor = addInstructor(schema);
        Entity lesson = addLesson(schema);
        Entity student = addStudent(schema);

        Property placeId = lesson.addLongProperty("pid").notNull().getProperty();
        Property instructorId = lesson.addLongProperty("iid").notNull().getProperty();

        lesson.addToOne(place, placeId);
        lesson.addToOne(instructor, instructorId);
    }

    private static Entity addInstructor(final Schema schema) {
        Entity instructor = schema.addEntity("Instructor");
        instructor.addIdProperty().primaryKey();
        instructor.addStringProperty("name");
        instructor.addStringProperty("jobs");
        instructor.addStringProperty("email");
        instructor.addStringProperty("phone");
        instructor.addStringProperty("password");
        //instructor.addDateProperty("createAt");
        //instructor.addDateProperty("updateAt");

        return instructor;
    }

    private static Entity addStudent(final Schema schema) {
        Entity instructor = schema.addEntity("Student");
        instructor.addIdProperty().primaryKey();
        instructor.addStringProperty("name");
        instructor.addStringProperty("email");
        instructor.addStringProperty("phone");
        instructor.addStringProperty("password");
        //instructor.addDateProperty("createAt");
        //instructor.addDateProperty("updateAt");

        return instructor;
    }

    private static Entity addPlace(final Schema schema) {
        Entity place = schema.addEntity("Place");
        place.addIdProperty().primaryKey();
        place.addStringProperty("name");
        place.addStringProperty("uuid");

        return place;
    }

    private static Entity addLesson(final Schema schema) {
        Entity lesson = schema.addEntity("Lesson");
        lesson.addIdProperty().primaryKey();
        lesson.addStringProperty("name");
        lesson.addStringProperty("desc");
        lesson.addIntProperty("personnel");
        lesson.addDateProperty("createAt");
        lesson.addDateProperty("updateAt");

        return lesson;
    }
}
