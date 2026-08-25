import re

with open('src/main/java/com/bidding/serviceImpl/InspectionServiceImpl.java', 'r', encoding='utf-8') as f:
    content = f.read()

target = '''            wsMessage.put("totalBids", v.getTotalBids() != null ? v.getTotalBids() : 0);

            webSocketHandler.broadcast(id, wsMessage);
        }'''

replacement = '''            wsMessage.put("totalBids", v.getTotalBids() != null ? v.getTotalBids() : 0);

            webSocketHandler.broadcast(id, wsMessage);
            
            String vehicleTitle = String.format("%s %s (%s)", v.getBrand(), v.getModel(), v.getVehicleNumber());
            
            // Notify WINNER if there is one
            if (v.getCurrentHighestBidder() != null) {
                notificationService.createNotification(
                        "DEALER",
                        v.getCurrentHighestBidder().getEmail(),
                        id,
                        "Auction Won: " + vehicleTitle,
                        "Congratulations! You won the auction for " + vehicleTitle + " with a bid of ₹" + String.format("%,.0f", v.getCurrentHighestBid() != null ? v.getCurrentHighestBid() : 0.0) + ".",
                        "AUCTION_WON"
                );
            }

            // Notify ALL DEALERS that it ended
            notificationService.createNotification(
                    "DEALER",
                    "ALL",
                    id,
                    "Auction Ended: " + vehicleTitle,
                    "The live auction for " + vehicleTitle + " has ended.",
                    "AUCTION_ENDED"
            );
        }'''

content = content.replace(target, replacement)

with open('src/main/java/com/bidding/serviceImpl/InspectionServiceImpl.java', 'w', encoding='utf-8') as f:
    f.write(content)
