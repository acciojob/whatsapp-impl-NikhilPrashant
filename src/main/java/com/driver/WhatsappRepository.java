package com.driver;

import java.util.*;

import org.springframework.stereotype.Repository;

@Repository
public class WhatsappRepository {
    private HashMap<Group, List<User>> groupUserMap;
    private HashMap<Group, List<Message>> groupMessageMap;
    private HashMap<Message, User> senderMap;
    private HashMap<Group, User> adminMap;
    private HashSet<String> userMobile;
    private int customGroupCount;
    private int messageId;

    public WhatsappRepository() {
        this.groupMessageMap = new HashMap<Group, List<Message>>();
        this.groupUserMap = new HashMap<Group, List<User>>();
        this.senderMap = new HashMap<Message, User>();
        this.adminMap = new HashMap<Group, User>();
        this.userMobile = new HashSet<>();
        this.customGroupCount = 0;
        this.messageId = 0;
    }

    public String createUser(String name, String mobileNumber) throws Exception {
        if (userMobile.contains(mobileNumber)) throw new Exception("User Already Exists In Database");
        else {
            User user = new User(name, mobileNumber);
            userMobile.add(mobileNumber);
        }
        return "User Added In Database";
    }

    public Group createGroup(List<User> userList) {
        if (userList.size() < 2) return null;
        if (userList.size() == 2) {
            Group group = new Group(userList.get(0).getName(), 2);
            adminMap.put(group, userList.get(0));
            groupUserMap.put(group, userList);
            groupMessageMap.put(group, new ArrayList<Message>());
            return group;
        }
        this.customGroupCount += 1;
        Group group = new Group(new String("Group " + customGroupCount), userList.size());
        adminMap.put(group, userList.get(0));
        groupUserMap.put(group, userList);
        groupMessageMap.put(group, new ArrayList<Message>());
        return group;
    }

    public int createMessage(String messageContent) {
        messageId += 1;
        Message message = new Message(messageId, messageContent);
        return messageId;
    }

    public int sendMessage(Message messageContent, User sender, Group group) throws Exception {
        if (adminMap.containsKey(group)) {
            List<User> users = groupUserMap.get(group);
            Boolean userFound = false;
            for (User user : users) {
                if (user.equals(sender)) {
                    userFound = true;
                    break;
                }
            }
            if (userFound) {
                senderMap.put(messageContent, sender);
                List<Message> messages = groupMessageMap.get(group);
                messages.add(messageContent);
                groupMessageMap.put(group, messages);
                return messages.size();
            }
            throw new Exception("You are not allowed to send message");
        }
        throw new Exception("Group does not exist");
    }

    public String changeAdmin(User approver, User user, Group group) throws Exception {
        if (!adminMap.containsKey(group)) throw new Exception("Group does not exist");
        if (!adminMap.get(group).equals(approver)) throw new Exception("Approver does not have rights");
        List<User> participants = groupUserMap.get(group);
        Boolean userFound = false;
        for (User participant : participants) {
            if (participant.equals(user)) {
                userFound = true;
                break;
            }
        }
        if (userFound) {
            adminMap.put(group, user);
            return "SUCCESS";
        }
        throw new Exception("User is not a participant");

    }

    public int removeUser(User user) throws Exception {
        Boolean userFound = false;
        Group userGroup = null;
        for (Group group : groupUserMap.keySet()) {
            List<User> participants = groupUserMap.get(group);
            for (User participant : participants) {
                if (participant.equals(user)) {
                    if (adminMap.get(group).equals(user)) {
                        throw new Exception("Cannot remove admin");
                    }
                    userGroup = group;
                    userFound = true;
                    break;
                }
            }
            if (userFound) {
                break;
            }
        }
        if (userFound) {
            List<User> users = groupUserMap.get(userGroup);
            List<User> updatedUsers = new ArrayList<>();
            for (User participant : users) {
                if (participant.equals(user))
                    continue;
                updatedUsers.add(participant);
            }
            groupUserMap.put(userGroup, updatedUsers);

            List<Message> messages = groupMessageMap.get(userGroup);
            List<Message> updatedMessages = new ArrayList<>();
            for (Message message : messages) {
                if (senderMap.get(message).equals(user))
                    continue;
                updatedMessages.add(message);
            }
            groupMessageMap.put(userGroup, updatedMessages);

            HashMap<Message, User> updatedSenderMap = new HashMap<>();
            for (Message message : senderMap.keySet()) {
                if (senderMap.get(message).equals(user))
                    continue;
                updatedSenderMap.put(message, senderMap.get(message));
            }
            senderMap = updatedSenderMap;
            return updatedUsers.size() + updatedMessages.size() + updatedSenderMap.size();
        }
        throw new Exception("User not found");
    }

    public String findMessage(Date start, Date end, int K) throws Exception {
        List<Message> messages = new ArrayList<>();
        for (Group group : groupMessageMap.keySet()) {
            messages.addAll(groupMessageMap.get(group));
        }
        List<Message> filteredMessages = new ArrayList<>();
        for (Message message : messages) {
            if (message.getTimestamp().after(start) && message.getTimestamp().before(end)) {
                filteredMessages.add(message);
            }
        }
        if (filteredMessages.size() < K) {
            throw new Exception("K is greater than the number of messages");
        }
        Collections.sort(filteredMessages, new Comparator<Message>() {
            public int compare(Message m1, Message m2) {
                return m2.getTimestamp().compareTo(m1.getTimestamp());
            }
        });
        return filteredMessages.get(K - 1).getContent();
    }
}
