import re

with open('src/main/java/com/bidding/serviceImpl/InspectionServiceImpl.java', 'r', encoding='utf-8') as f:
    content = f.read()

target = '''            } else if ("LIVE".equalsIgnoreCase(vehicleStatus)) {
                notificationService.createNotification(
                        "ALL_DEALERS",
                        null,'''

replacement = '''            } else if ("LIVE".equalsIgnoreCase(vehicleStatus)) {
                notificationService.createNotification(
                        "DEALER",
                        "ALL",'''

content = content.replace(target, replacement)

with open('src/main/java/com/bidding/serviceImpl/InspectionServiceImpl.java', 'w', encoding='utf-8') as f:
    f.write(content)
