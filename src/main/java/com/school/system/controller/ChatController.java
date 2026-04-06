package com.school.system.controller;

import com.school.system.entity.*;
import com.school.system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/chat")
@CrossOrigin
public class ChatController {

    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private ClassInfoRepository classInfoRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private ParentRepository parentRepository;

    @GetMapping("/contacts")
    public Map<String, Object> getContacts(@RequestParam String role, @RequestParam Integer userId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, String>> groups = new ArrayList<>();
        List<Map<String, String>> users = new ArrayList<>();

        List<ClassInfo> allClasses = classInfoRepository.findAll();

        //1. 获取群聊列表
        if ("student".equals(role)) {
            studentRepository.findById(userId).ifPresent(s -> {
                allClasses.stream().filter(c -> c.getClassId().equals(s.getClassId())).forEach(c -> addGroup(groups, c));
            });
        } else if ("parent".equals(role)) {
            List<Student> children = studentRepository.findByParentId(userId);
            List<Integer> childClassIds = children.stream().map(Student::getClassId).toList();
            allClasses.stream().filter(c -> childClassIds.contains(c.getClassId())).forEach(c -> addGroup(groups, c));
        } else if ("headmaster".equals(role)) {
            allClasses.stream().filter(c -> userId.equals(c.getHeadmasterId())).forEach(c -> addGroup(groups, c));
        } else if ("teacher".equals(role)) {
            allClasses.stream().filter(c -> c.getTeacherIds() != null && Arrays.asList(c.getTeacherIds().split(",")).contains(String.valueOf(userId)))
                    .forEach(c -> addGroup(groups, c));
        } else if ("admin".equals(role)) {
            allClasses.forEach(c -> addGroup(groups, c));
        }

        //2. 获取私聊联系人
        List<Teacher> allTeachers = teacherRepository.findAll();
        List<Teacher> visibleTeachers = new ArrayList<>();

        if ("student".equals(role) || "parent".equals(role)) {
            //学生/家长：只能看到本班的班主任和任课老师
            Integer myClassId = null;
            if ("student".equals(role)) {
                Student s = studentRepository.findById(userId).orElse(null);
                if (s != null) myClassId = s.getClassId();
            } else {
                List<Student> children = studentRepository.findByParentId(userId);
                if (!children.isEmpty()) myClassId = children.get(0).getClassId();
            }

            if (myClassId != null) {
                ClassInfo myClass = classInfoRepository.findById(myClassId).orElse(null);
                if (myClass != null) {
                    for (Teacher t : allTeachers) {
                        boolean isMyHeadmaster = t.getTeacherId().equals(myClass.getHeadmasterId());
                        boolean isMyTeacher = myClass.getTeacherIds() != null && Arrays.asList(myClass.getTeacherIds().split(",")).contains(t.getTeacherId().toString());
                        if (isMyHeadmaster || isMyTeacher) visibleTeachers.add(t);
                    }
                }
            }
        } else if ("headmaster".equals(role) || "teacher".equals(role)) {
            //教师/班主任视角：只能看到跟自己同教一个班的同事
            Set<Integer> colleagueIds = new HashSet<>();
            for (ClassInfo c : allClasses) {
                boolean isMyClass = false;
                if ("headmaster".equals(role) && userId.equals(c.getHeadmasterId())) isMyClass = true;
                if ("teacher".equals(role) && c.getTeacherIds() != null && Arrays.asList(c.getTeacherIds().split(",")).contains(userId.toString())) isMyClass = true;

                if (isMyClass) {
                    if (c.getHeadmasterId() != null) colleagueIds.add(c.getHeadmasterId());
                    if (c.getTeacherIds() != null && !c.getTeacherIds().trim().isEmpty()) {
                        for (String tId : c.getTeacherIds().split(",")) {
                            colleagueIds.add(Integer.parseInt(tId.trim()));
                        }
                    }
                }
            }
            // 过滤出真正有交集的同事
            for (Teacher t : allTeachers) {
                if (colleagueIds.contains(t.getTeacherId())) {
                    visibleTeachers.add(t);
                }
            }
        } else {
            //教务处/管理员视角：全局透视，看所有人
            visibleTeachers = allTeachers;
        }

        // 把计算出来的合规老师装进列表
        visibleTeachers.stream().filter(t -> "headmaster".equals(t.getRole())).forEach(t -> addUser(users, "teacher_" + t.getTeacherId(), t.getName() + " (⭐班主任)"));
        visibleTeachers.stream().filter(t -> !"headmaster".equals(t.getRole())).forEach(t -> {
            String subject = (t.getSubject() != null && !t.getSubject().trim().isEmpty()) ? t.getSubject() : "普通";
            addUser(users, "teacher_" + t.getTeacherId(), t.getName() + " (" + subject + "教师)");
        });

        // 3. 过滤学生和家长名单
        if ("admin".equals(role)) {
            // 超管看全校
            studentRepository.findAll().forEach(s -> addUser(users, "student_" + s.getStudentId(), s.getName() + " (学生)"));
            parentRepository.findAll().forEach(p -> addUser(users, "parent_" + p.getParentId(), p.getName() + " (家长)"));
        } else if ("headmaster".equals(role) || "teacher".equals(role)) {
            // 老师/班主任只能看到自己班里的学生和这些学生的家长
            Set<Integer> myClassIds = new HashSet<>();
            for (ClassInfo c : allClasses) {
                if ("headmaster".equals(role) && userId.equals(c.getHeadmasterId())) myClassIds.add(c.getClassId());
                if ("teacher".equals(role) && c.getTeacherIds() != null && Arrays.asList(c.getTeacherIds().split(",")).contains(userId.toString())) myClassIds.add(c.getClassId());
            }

            // 过滤学生
            studentRepository.findAll().stream().filter(s -> myClassIds.contains(s.getClassId())).forEach(s -> addUser(users, "student_" + s.getStudentId(), s.getName() + " (学生)"));

            // 过滤家长
            Set<Integer> myParentIds = studentRepository.findAll().stream()
                    .filter(s -> myClassIds.contains(s.getClassId()) && s.getParentId() != null)
                    .map(Student::getParentId).collect(Collectors.toSet());

            parentRepository.findAll().stream().filter(p -> myParentIds.contains(p.getParentId())).forEach(p -> {
                String relation = p.getRelation() != null ? p.getRelation() : "家长";
                addUser(users, "parent_" + p.getParentId(), p.getName() + " (" + relation + ")");
            });
        }

        result.put("groups", groups);
        result.put("users", users);
        return result;
    }

    private void addGroup(List<Map<String, String>> groups, ClassInfo c) {
        Map<String, String> g = new HashMap<>();
        g.put("id", String.valueOf(c.getClassId()));
        g.put("name", c.getClassName() + " 群聊");
        g.put("type", "GROUP");

        int memberCount = 0;
        if (c.getHeadmasterId() != null) memberCount++;
        if (c.getTeacherIds() != null && !c.getTeacherIds().trim().isEmpty()) {
            memberCount += c.getTeacherIds().split(",").length;
        }
        List<Student> classStudents = studentRepository.findByClassId(c.getClassId());
        memberCount += classStudents.size();

        long parentCount = classStudents.stream()
                .map(Student::getParentId)
                .filter(id -> id != null)
                .distinct()
                .count();
        memberCount += parentCount;

        g.put("memberCount", String.valueOf(memberCount));
        groups.add(g);
    }

    private void addUser(List<Map<String, String>> users, String id, String name) {
        Map<String, String> u = new HashMap<>();
        u.put("id", id);
        u.put("name", name);
        u.put("type", "PRIVATE");
        users.add(u);
    }

    @GetMapping("/history")
    public List<ChatMessage> getHistory() {
        return chatMessageRepository.findAll();
    }
}