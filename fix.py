import re

with open('src/main/java/com/bidding/serviceImpl/InspectionServiceImpl.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Fix the SOLD OUT notification to also notify DEALER_ALL
target = '"STATUS_UPDATE"\n                );\n            } else if ("LIVE".equalsIgnoreCase(vehicleStatus)) {'
replacement = '''"STATUS_UPDATE"
                );
                
                // Notify all dealers that the auction has ended
                notificationService.createNotification(
                        "DEALER",
                        "ALL",
                        id,
                        "Auction Ended: " + vehicleTitle,
                        vehicleTitle + " has been marked as SOLD OUT. The auction is now closed.",
                        "AUCTION_ENDED"
                );
            } else if ("LIVE".equalsIgnoreCase(vehicleStatus)) {'''

content = content.replace(target, replacement)

# Clean up broken emoji text
content = re.sub(r'A.*?Auction Won:', 'Auction Won:', content)
content = re.sub(r'A.*?for A.*?1"', 'for ₹"', content)
content = re.sub(r'A.*?Vehicle Marked SOLD OUT:', 'Vehicle Marked SOLD OUT:', content)
content = re.sub(r'A.*?Live Auction Started:', 'Live Auction Started:', content)

with open('src/main/java/com/bidding/serviceImpl/InspectionServiceImpl.java', 'w', encoding='utf-8') as f:
    f.write(content)
