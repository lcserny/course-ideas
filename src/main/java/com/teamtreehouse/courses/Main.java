package com.teamtreehouse.courses;

import com.teamtreehouse.courses.model.CourseIdea;
import com.teamtreehouse.courses.model.CourseIdeaDAO;
import com.teamtreehouse.courses.model.NotFoundException;
import com.teamtreehouse.courses.model.SimpleCourseIdeaDAO;
import spark.ModelAndView;
import spark.Request;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

/**
 * Created by user on 03.05.2016.
 */
public class Main
{
    public static final String FLASH_MESSAGE_KEY = "flash_message";

    public static void main(String[] args)
    {
        staticFileLocation("/public");

        CourseIdeaDAO dao = new SimpleCourseIdeaDAO();

        before((request, response) -> {
            if (request.cookie("username") != null) {
                request.attribute("username", request.cookie("username"));
            }
        });

        before("/ideas", (request, response) -> {
            if (request.attribute("username") == null) {
                setFlashMessage(request, "Oops! Please sign in first!");
                response.redirect("/");
                halt();
            }
        });

        get("/", (req, res) -> {
            ModelMap model = new ModelMap(req);
            model.put("username", req.cookie("username"));
            return new ModelAndView(model, "index.hbs");
        }, new HandlebarsTemplateEngine());

        post("/sign-in", (request, response) -> {
            String username = request.queryParams("username");
            response.cookie("username", username);
            response.redirect("/");
            return null;
        });

        get("/ideas", (request, response) -> {
            ModelMap model = new ModelMap(request);
            model.put("ideas", dao.findAll());
            return new ModelAndView(model, "ideas.hbs");
        }, new HandlebarsTemplateEngine());

        post("/ideas", (request, response) -> {
            String title = request.queryParams("title");
            CourseIdea courseIdea = new CourseIdea(title, request.attribute("username"));
            dao.add(courseIdea);
            response.redirect("/ideas");
            return null;
        });

        get("/ideas/:slug", (request, response) -> {
            Map<String, Object> model = new HashMap<>();
            CourseIdea courseIdea = dao.findBySlug(request.params("slug"));
            model.put("idea", courseIdea);
            return new ModelAndView(model, "idea.hbs");
        }, new HandlebarsTemplateEngine());

        post("/ideas/:slug/vote", (request, response) -> {
            CourseIdea courseIdea = dao.findBySlug(request.params("slug"));
            boolean added = courseIdea.addVoter(request.attribute("username"));
            if (added) {
                setFlashMessage(request, "Thanks for your vote!");
            } else {
                setFlashMessage(request, "You have already voted!");
            }
            response.redirect("/ideas");
            return null;
        });

        exception(NotFoundException.class, (exception, request, response) -> {
            response.status(404);
            HandlebarsTemplateEngine engine = new HandlebarsTemplateEngine();
            String html = engine.render(new ModelAndView(null, "not-found.hbs"));
            response.body(html);
        });
    }

    private static void setFlashMessage(Request request, String message)
    {
        request.session().attribute(FLASH_MESSAGE_KEY, message);
    }

    private static String getFlashMessage(Request request)
    {
        if (request.session(false) == null) {
            return  null;
        }

        if (!request.session().attributes().contains(FLASH_MESSAGE_KEY)) {
            return null;
        }

        return (String) request.session().attribute(FLASH_MESSAGE_KEY);
    }

    private static String captureFlashMessage(Request request)
    {
        String message = getFlashMessage(request);
        if (message != null) {
            request.session().removeAttribute(FLASH_MESSAGE_KEY);
        }
        return message;
    }

    public static class ModelMap extends HashMap<String, Object>
    {
        public static final String FLASH_MESSAGE = "flashMessage";

        public ModelMap(Request request)
        {
            put(FLASH_MESSAGE, captureFlashMessage(request));
        }
    }
}
