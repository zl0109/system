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
        Set<String> addedUserIds = new HashSet<>();

        List<ClassInfo> allClasses = classInfoRepository.findAll();

        // ================= 1. 获取群聊列表 =================
        if ("student".equals(role)) {
            Student student = studentRepository.findById(userId).orElse(null);
            if (student != null && student.getClassId() != null) {
                for (ClassInfo c : allClasses) {
                    if (c.getClassId().equals(student.getClassId())) {
                        addGroup(groups, c);
                    }
                }
            }

        } else if ("parent".equals(role)) {
            List<Student> children = studentRepository.findByParentId(userId);
            Set<Integer> childClassIds = children.stream()
                    .map(Student::getClassId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            for (ClassInfo c : allClasses) {
                if (childClassIds.contains(c.getClassId())) {
                    addGroup(groups, c);
                }
            }

        } else if ("headmaster".equals(role)) {
            for (ClassInfo c : allClasses) {
                if (userId.equals(c.getHeadmasterId())) {
                    addGroup(groups, c);
                }
            }

        } else if ("teacher".equals(role)) {
            for (ClassInfo c : allClasses) {
                if (isTeacherInClass(c, userId)) {
                    addGroup(groups, c);
                }
            }

        } else if ("admin".equals(role) || "department".equals(role) || "leadership".equals(role)) {
            for (ClassInfo c : allClasses) {
                addGroup(groups, c);
            }
        }

        // ================= 2. 获取当前用户可见班级 =================
        Set<Integer> visibleClassIds = new HashSet<>();

        if ("student".equals(role)) {
            Student student = studentRepository.findById(userId).orElse(null);
            if (student != null && student.getClassId() != null) {
                visibleClassIds.add(student.getClassId());
            }

        } else if ("parent".equals(role)) {
            List<Student> children = studentRepository.findByParentId(userId);
            for (Student child : children) {
                if (child.getClassId() != null) {
                    visibleClassIds.add(child.getClassId());
                }
            }

        } else if ("headmaster".equals(role)) {
            for (ClassInfo c : allClasses) {
                if (userId.equals(c.getHeadmasterId())) {
                    visibleClassIds.add(c.getClassId());
                }
            }

        } else if ("teacher".equals(role)) {
            for (ClassInfo c : allClasses) {
                if (isTeacherInClass(c, userId)) {
                    visibleClassIds.add(c.getClassId());
                }
            }

        } else if ("admin".equals(role) || "department".equals(role) || "leadership".equals(role)) {
            for (ClassInfo c : allClasses) {
                visibleClassIds.add(c.getClassId());
            }
        }

        // ================= 3. 添加教师联系人 =================
        if ("student".equals(role) || "parent".equals(role)) {
            // 学生、家长：都可以看到所在班级的班主任和任课教师
            for (ClassInfo c : allClasses) {
                if (!visibleClassIds.contains(c.getClassId())) {
                    continue;
                }

                addClassTeachers(users, addedUserIds, c, null);
            }

        } else if ("headmaster".equals(role) || "teacher".equals(role)) {
            // 教师、班主任：可以看到同班共事教师，但不显示自己
            for (ClassInfo c : allClasses) {
                if (!visibleClassIds.contains(c.getClassId())) {
                    continue;
                }

                addClassTeachers(users, addedUserIds, c, "teacher_" + userId);
            }

        } else if ("admin".equals(role) || "department".equals(role) || "leadership".equals(role)) {
            // 管理员、部门、领导：可以看到所有教师
            for (Teacher t : teacherRepository.findAll()) {
                addTeacherUser(users, addedUserIds, t, null);
            }
        }

        // ================= 4. 添加学生和家长联系人 =================

        if ("admin".equals(role) || "department".equals(role) || "leadership".equals(role)) {
            // 管理员、部门、领导：显示全校学生和家长
            for (Student s : studentRepository.findAll()) {
                addUserIfAbsent(users, addedUserIds,
                        "student_" + s.getStudentId(),
                        s.getName() + " (学生)");
            }

            for (Parent p : parentRepository.findAll()) {
                String relation = getParentRelation(p);
                addUserIfAbsent(users, addedUserIds,
                        "parent_" + p.getParentId(),
                        p.getName() + " (" + relation + ")");
            }

        } else if ("headmaster".equals(role) || "teacher".equals(role)) {
            // 教师、班主任：显示自己相关班级的学生和家长
            List<Student> visibleStudents = studentRepository.findAll().stream()
                    .filter(s -> s.getClassId() != null && visibleClassIds.contains(s.getClassId()))
                    .collect(Collectors.toList());

            addStudentsAndParents(users, addedUserIds, visibleStudents, null);

        } else if ("parent".equals(role)) {
            // 家长：显示绑定学生所在班级的所有学生和家长，但不显示自己
            List<Student> visibleStudents = studentRepository.findAll().stream()
                    .filter(s -> s.getClassId() != null && visibleClassIds.contains(s.getClassId()))
                    .collect(Collectors.toList());

            addStudentsAndParents(users, addedUserIds, visibleStudents, "parent_" + userId);

        } else if ("student".equals(role)) {
            // 学生：不添加学生和家长联系人
            // 学生私聊联系人只保留上面已经添加的本班教师
        }

        result.put("groups", groups);
        result.put("users", users);
        return result;
    }

    private void addClassTeachers(List<Map<String, String>> users,
                                  Set<String> addedUserIds,
                                  ClassInfo classInfo,
                                  String excludeId) {
        // 添加班主任
        if (classInfo.getHeadmasterId() != null) {
            Teacher headmaster = teacherRepository.findById(classInfo.getHeadmasterId()).orElse(null);
            if (headmaster != null) {
                addTeacherUser(users, addedUserIds, headmaster, excludeId);
            }
        }

        // 添加任课教师
        if (classInfo.getTeacherIds() != null && !classInfo.getTeacherIds().trim().isEmpty()) {
            String[] teacherIds = classInfo.getTeacherIds().split(",");
            for (String tid : teacherIds) {
                try {
                    Integer teacherId = Integer.valueOf(tid.trim());
                    Teacher teacher = teacherRepository.findById(teacherId).orElse(null);
                    if (teacher != null) {
                        addTeacherUser(users, addedUserIds, teacher, excludeId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void addTeacherUser(List<Map<String, String>> users,
                                Set<String> addedUserIds,
                                Teacher teacher,
                                String excludeId) {
        String contactId = "teacher_" + teacher.getTeacherId();

        if (contactId.equals(excludeId)) {
            return;
        }

        String name;
        if ("headmaster".equals(teacher.getRole())) {
            name = teacher.getName() + " (⭐班主任)";
        } else {
            String subject = teacher.getSubject() != null && !teacher.getSubject().trim().isEmpty()
                    ? teacher.getSubject()
                    : "普通";
            name = teacher.getName() + " (" + subject + "教师)";
        }

        addUserIfAbsent(users, addedUserIds, contactId, name);
    }

    private void addStudentsAndParents(List<Map<String, String>> users,
                                       Set<String> addedUserIds,
                                       List<Student> visibleStudents,
                                       String excludeParentId) {
        // 添加学生
        for (Student s : visibleStudents) {
            addUserIfAbsent(users, addedUserIds,
                    "student_" + s.getStudentId(),
                    s.getName() + " (学生)");
        }

        // 添加家长
        Set<Integer> parentIds = visibleStudents.stream()
                .map(Student::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Parent p : parentRepository.findAll()) {
            if (!parentIds.contains(p.getParentId())) {
                continue;
            }

            String contactId = "parent_" + p.getParentId();

            if (contactId.equals(excludeParentId)) {
                continue;
            }

            String relation = getParentRelation(p);
            String childName = findChildNameByParentId(visibleStudents, p.getParentId());

            if (childName != null) {
                addUserIfAbsent(users, addedUserIds,
                        contactId,
                        p.getName() + " (" + childName + relation + ")");
            } else {
                addUserIfAbsent(users, addedUserIds,
                        contactId,
                        p.getName() + " (" + relation + ")");
            }
        }
    }

    private boolean isTeacherInClass(ClassInfo c, Integer teacherId) {
        if (c == null || teacherId == null || c.getTeacherIds() == null) {
            return false;
        }

        String[] ids = c.getTeacherIds().split(",");
        for (String id : ids) {
            if (id.trim().equals(String.valueOf(teacherId))) {
                return true;
            }
        }

        return false;
    }

    private String getParentRelation(Parent p) {
        if (p.getRelation() != null && !p.getRelation().trim().isEmpty()) {
            return p.getRelation();
        }
        return "家长";
    }

    private String findChildNameByParentId(List<Student> students, Integer parentId) {
        for (Student s : students) {
            if (s.getParentId() != null && s.getParentId().equals(parentId)) {
                return s.getName();
            }
        }
        return null;
    }

    private void addGroup(List<Map<String, String>> groups, ClassInfo c) {
        Map<String, String> g = new HashMap<>();
        g.put("id", String.valueOf(c.getClassId()));
        g.put("name", c.getClassName() + " 群聊");
        g.put("type", "GROUP");

        int memberCount = 0;

        if (c.getHeadmasterId() != null) {
            memberCount++;
        }

        if (c.getTeacherIds() != null && !c.getTeacherIds().trim().isEmpty()) {
            memberCount += c.getTeacherIds().split(",").length;
        }

        List<Student> classStudents = studentRepository.findByClassId(c.getClassId());
        memberCount += classStudents.size();

        long parentCount = classStudents.stream()
                .map(Student::getParentId)
                .filter(Objects::nonNull)
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

    private void addUserIfAbsent(List<Map<String, String>> users,
                                 Set<String> addedUserIds,
                                 String id,
                                 String name) {
        if (id == null || addedUserIds.contains(id)) {
            return;
        }

        addedUserIds.add(id);
        addUser(users, id, name);
    }

    @GetMapping("/history")
    public List<ChatMessage> getHistory(@RequestParam String myUniqueId,
                                        @RequestParam(required = false) String classIds) {

        List<ChatMessage> allMsgs = chatMessageRepository.findAll();

        List<String> myGroups = new ArrayList<>();
        if (classIds != null && !classIds.trim().isEmpty()) {
            myGroups = Arrays.asList(classIds.split(","));
        }

        List<String> finalMyGroups = myGroups;

        return allMsgs.stream().filter(m -> {
            boolean isMyPrivate = "PRIVATE".equals(m.getChatType())
                    && (myUniqueId.equals(m.getSenderId()) || myUniqueId.equals(m.getTargetId()));

            boolean isMyGroup = "GROUP".equals(m.getChatType())
                    && finalMyGroups.contains(m.getTargetId());

            return isMyPrivate || isMyGroup;
        }).collect(Collectors.toList());
    }
}