package com.teamtreehouse.courses.model;

import java.util.List;

/**
 * Created by user on 03.05.2016.
 */
public interface CourseIdeaDAO
{
    boolean add(CourseIdea idea);

    List<CourseIdea> findAll();

    CourseIdea findBySlug(String slug);
}
