import re

with open('src/main/java/com/bidding/serviceImpl/InspectionServiceImpl.java', 'r', encoding='utf-8') as f:
    content = f.read()

target = '''            wsMessage.put("currentHighestBid", v.getCurrentHighestBid());
            wsMessage.put("auctionEndTime", v.getAuctionEndTime().toString());
            webSocketHandler.broadcast(id, wsMessage);
        }
    }'''

replacement = '''            wsMessage.put("currentHighestBid", v.getCurrentHighestBid());
            wsMessage.put("auctionEndTime", v.getAuctionEndTime().toString());
            webSocketHandler.broadcast(id, wsMessage);
            
            String vehicleTitle = String.format("%s %s (%s)", v.getBrand(), v.getModel(), v.getVehicleNumber());
            notificationService.createNotification(
                    "DEALER",
                    "ALL",
                    id,
                    "Live Auction Started: " + vehicleTitle,
                    "Bidding is now LIVE for " + vehicleTitle + "! Place your bids now.",
                    "AUCTION_LIVE"
            );
        }
    }'''

content = content.replace(target, replacement)

with open('src/main/java/com/bidding/serviceImpl/InspectionServiceImpl.java', 'w', encoding='utf-8') as f:
    f.write(content)
