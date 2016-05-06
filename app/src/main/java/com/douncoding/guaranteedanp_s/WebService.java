package com.douncoding.guaranteedanp_s;

import com.douncoding.dao.Instructor;
import com.douncoding.dao.Lesson;
import com.douncoding.dao.LessonTime;
import com.douncoding.dao.Place;
import com.douncoding.dao.Student;

import java.util.List;

import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface WebService {
    @GET("/lessons")
    Call<List<Lesson>> loadLessons();

    /**
     * 강사번호를 통해 강사정보를 조회
     * @param id 강사번호
     * @return 강사정보
     */
    @GET("/instructors/{id}")
    Call<Instructor> loadInstructor(@Path("id") int id);

    /**
     * 등록된 모든 강사정보를 조회
     * @return 강사목록
     */
    @GET("/instructors/all")
    Call<List<Instructor>> loadAllInstructors();

    /**
     * 등록된 모든 장소목록
     * @return 강의실 목록
     */
    @GET("/place")
    Call<List<Place>> loadAllPlaces();

    /**
     * 등록된 모든 강의목록
     * @return 강의목록
     */
    @GET("/lessons")
    Call<List<Lesson>> loadAllLessons();

    /**
     *
     */
    @GET("/lessons/times")
    Call<List<LessonTime>> getLessonTimes(@Query("lessonName") String lessonName);

    /**
     * 학생 로그인
     * @param email 가입한 주소
     * @param password 비밀번호
     * @return 로그인한 학생정보
     */
    @GET("/students/{email}/login")
    Call<Student> login(@Path("email") String email, @Query("password") String password);

    /**
     * 수강신청
     * @param lessonId 강의번호
     * @param studentId 학생번호
     * @return 단순 성공/실패
     */
    @POST("/enrollments/lessons/{lid}/students/{sid}")
    Call<ResponseBody> enrollment(@Path("lid") int lessonId, @Path("sid") int studentId);

    /**
     *
     * @param studentId
     * @return
     */
    @GET("/enrollments")
    Call<List<Lesson>> getEnrollmentLesson(@Query("sid") int studentId);
}
