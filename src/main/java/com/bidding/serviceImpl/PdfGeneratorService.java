package com.bidding.serviceImpl;

import com.bidding.entity.*;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.bidding.enums.PhotoType;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfGeneratorService {

    public byte[] generateInspectionPdf(Inspection inspection, 
                                         List<InspectionPanel> panels,
                                         MechanicalInspection mechanical, 
                                         TyreInspection tyres, 
                                         InteriorInspection interior, 
                                         List<InspectionImage> images,
                                         InspectionRemarks remarks) {
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        // Styling Colors
        Color primaryColor = new Color(255, 199, 0); // Gold / Yellow
        Color darkColor = new Color(13, 14, 18); // Dark Charcoal
        Color lightBg = new Color(245, 245, 247); // Premium Light Gray
        Color borderGray = new Color(220, 220, 225);

        // Styling Fonts
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.BOLD, darkColor);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, darkColor);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, darkColor);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Header Band
            PdfPTable headerTable = new PdfPTable(1);
            headerTable.setWidthPercentage(100);
            headerTable.setSpacingAfter(15);
            
            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(darkColor);
            headerCell.setPadding(12);
            headerCell.setBorder(Rectangle.NO_BORDER);
            
            Paragraph brand = new Paragraph("CARYANAM", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Font.BOLD, primaryColor));
            brand.setAlignment(Element.ALIGN_CENTER);
            headerCell.addElement(brand);
            
            Paragraph subtitle = new Paragraph("VEHICLE REMARKETING PLATFORM  ·  200-POINT CERTIFIED REPORT", 
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, Color.WHITE));
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingBefore(4);
            headerCell.addElement(subtitle);
            
            headerTable.addCell(headerCell);
            document.add(headerTable);

            // 2. Summary Rating Cards
            addSectionHeading(document, "EVALUATION RATINGS SUMMARY", sectionFont, lightBg, primaryColor);
            PdfPTable ratingsTable = new PdfPTable(4);
            ratingsTable.setWidthPercentage(100);
            ratingsTable.setSpacingAfter(15);

            addRatingCell(ratingsTable, "Exterior Rating", inspection.getExteriorRating(), primaryColor, borderGray);
            addRatingCell(ratingsTable, "Mechanical Rating", inspection.getMechanicalRating(), primaryColor, borderGray);
            addRatingCell(ratingsTable, "Tyres Rating", inspection.getTyreRating(), primaryColor, borderGray);
            addRatingCell(ratingsTable, "Interior Rating", inspection.getInteriorRating(), primaryColor, borderGray);
            document.add(ratingsTable);

            // 3. Vehicle Specifications
            addSectionHeading(document, "1. VEHICLE SPECIFICATIONS", sectionFont, lightBg, primaryColor);
            PdfPTable vehTable = new PdfPTable(2);
            vehTable.setWidthPercentage(100);
            vehTable.setSpacingAfter(15);

            Vehicle v = inspection.getVehicle();
            if (v != null) {
                addStyledTableCell(vehTable, "Vehicle Number", v.getVehicleNumber(), labelFont, valueFont, lightBg, borderGray);
                addStyledTableCell(vehTable, "Owner Name", v.getOwnerName(), labelFont, valueFont, lightBg, borderGray);
                addStyledTableCell(vehTable, "Brand & Model", v.getBrand() + " " + v.getModel(), labelFont, valueFont, lightBg, borderGray);
                addStyledTableCell(vehTable, "Variant", v.getVariant(), labelFont, valueFont, lightBg, borderGray);
                addStyledTableCell(vehTable, "Manufacturing Year", String.valueOf(v.getManufacturingYear()), labelFont, valueFont, lightBg, borderGray);
                addStyledTableCell(vehTable, "Fuel Type", v.getFuelType(), labelFont, valueFont, lightBg, borderGray);
                addStyledTableCell(vehTable, "Transmission", v.getTransmission(), labelFont, valueFont, lightBg, borderGray);
                addStyledTableCell(vehTable, "Odometer Reading", v.getOdometerReading() != null ? v.getOdometerReading() + " km" : "N/A", labelFont, valueFont, lightBg, borderGray);
                addStyledTableCell(vehTable, "Insurance Status", v.getInsuranceStatus(), labelFont, valueFont, lightBg, borderGray);
                addStyledTableCell(vehTable, "Inspection Date", v.getInspectionDate() != null ? v.getInspectionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "N/A", labelFont, valueFont, lightBg, borderGray);
            } else {
                PdfPCell cell = new PdfPCell(new Phrase("No specifications captured.", valueFont));
                cell.setColspan(2);
                cell.setPadding(8);
                vehTable.addCell(cell);
            }
            document.add(vehTable);

            // 4. Exterior Panel Checklist (rendered in 2 columns with images to save height)
            addSectionHeading(document, "2. EXTERIOR PANEL INSPECTION", sectionFont, lightBg, primaryColor);
            PdfPTable panelTable = new PdfPTable(6); // 2 sets of [Panel, Status, Photo]
            panelTable.setWidthPercentage(100);
            panelTable.setWidths(new float[]{2.2f, 1.3f, 1.5f, 2.2f, 1.3f, 1.5f});
            panelTable.setSpacingAfter(15);

            if (panels != null && !panels.isEmpty()) {
                for (int i = 0; i < panels.size(); i++) {
                    InspectionPanel p = panels.get(i);
                    addPanelCell(panelTable, p.getPanelName(), p.getCondition().name(), p.getImageUrl(), labelFont, valueFont, borderGray);
                }
                // complete empty cells if odd
                if (panels.size() % 2 != 0) {
                    panelTable.addCell("");
                    panelTable.addCell("");
                    panelTable.addCell("");
                }
            } else {
                PdfPCell cell = new PdfPCell(new Phrase("No panels evaluated.", valueFont));
                cell.setColspan(6);
                cell.setPadding(8);
                panelTable.addCell(cell);
            }
            document.add(panelTable);

            // 5. Mechanical Checklist
            addSectionHeading(document, "3. MECHANICAL DIAGNOSTICS", sectionFont, lightBg, primaryColor);
            PdfPTable mechTable = new PdfPTable(2);
            mechTable.setWidthPercentage(100);
            mechTable.setSpacingAfter(15);

            if (mechanical != null) {
                addStyledTableCell(mechTable, "Engine Status", mechanical.getEngineStatus(), labelFont, valueFont, lightBg, borderGray);
                addStyledTableCell(mechTable, "Engine Oil", mechanical.getEngineOil(), labelFont, valueFont, lightBg, borderGray);
                addStyledTableCell(mechTable, "Brake Oil", mechanical.getBrakeOil(), labelFont, valueFont, lightBg, borderGray);
                addStyledTableCell(mechTable, "Steering Oil", mechanical.getSteeringOil(), labelFont, valueFont, lightBg, borderGray);
                addStyledTableCell(mechTable, "Coolant", mechanical.getCoolant(), labelFont, valueFont, lightBg, borderGray);
                addStyledTableCell(mechTable, "Brake Booster", mechanical.getBrakeBooster(), labelFont, valueFont, lightBg, borderGray);
                addStyledTableCell(mechTable, "Brake Working", mechanical.getBrakeWorking(), labelFont, valueFont, lightBg, borderGray);
                addStyledTableCell(mechTable, "Suspension", mechanical.getSuspension(), labelFont, valueFont, lightBg, borderGray);
                addStyledTableCell(mechTable, "Fluid Leakage", mechanical.getFluidLeakage(), labelFont, valueFont, lightBg, borderGray);
                addStyledTableCell(mechTable, "Engine Noise / Smoke", mechanical.getEngineNoise() + " / " + mechanical.getSmoke(), labelFont, valueFont, lightBg, borderGray);
            } else {
                PdfPCell cell = new PdfPCell(new Phrase("No mechanical parameters captured.", valueFont));
                cell.setColspan(2);
                cell.setPadding(8);
                mechTable.addCell(cell);
            }
            document.add(mechTable);

            // 6. Tyres & Emergency
            addSectionHeading(document, "4. TYRES & EMERGENCY ACCESSORIES", sectionFont, lightBg, primaryColor);
            PdfPTable tyreTable = new PdfPTable(3);
            tyreTable.setWidthPercentage(100);
            tyreTable.setSpacingAfter(10);
            
            // Header row
            PdfPCell th1 = new PdfPCell(new Phrase("Position", labelFont)); th1.setBackgroundColor(lightBg); th1.setPadding(5); tyreTable.addCell(th1);
            PdfPCell th2 = new PdfPCell(new Phrase("Brand Name", labelFont)); th2.setBackgroundColor(lightBg); th2.setPadding(5); tyreTable.addCell(th2);
            PdfPCell th3 = new PdfPCell(new Phrase("Tread Remaining / Year", labelFont)); th3.setBackgroundColor(lightBg); th3.setPadding(5); tyreTable.addCell(th3);

            if (tyres != null) {
                addTyreRow(tyreTable, "Front Left", tyres.getFrontLeftBrand(), tyres.getFrontLeftTread() + "% / " + tyres.getFrontLeftYear(), valueFont);
                addTyreRow(tyreTable, "Front Right", tyres.getFrontRightBrand(), tyres.getFrontRightTread() + "% / " + tyres.getFrontRightYear(), valueFont);
                addTyreRow(tyreTable, "Rear Left", tyres.getRearLeftBrand(), tyres.getRearLeftTread() + "% / " + tyres.getRearLeftYear(), valueFont);
                addTyreRow(tyreTable, "Rear Right", tyres.getRearRightBrand(), tyres.getRearRightTread() + "% / " + tyres.getRearRightYear(), valueFont);
                addTyreRow(tyreTable, "Spare Wheel", tyres.getSpareBrand(), tyres.getSpareTread() + "% / " + tyres.getSpareYear(), valueFont);
            } else {
                PdfPCell cell = new PdfPCell(new Phrase("No tyre specifications captured.", valueFont));
                cell.setColspan(3);
                cell.setPadding(8);
                tyreTable.addCell(cell);
            }
            document.add(tyreTable);

            if (tyres != null) {
                Paragraph emergencyText = new Paragraph(String.format("Emergency Accessories: Jack (%s), Handle (%s), Toolkit (%s), Triangle (%s), First Aid Box (%s)",
                        getBooleanText(tyres.getHasJack()), getBooleanText(tyres.getHasHandle()), getBooleanText(tyres.getHasToolkit()),
                        getBooleanText(tyres.getHasTriangle()), getBooleanText(tyres.getHasFirstAidBox())), valueFont);
                emergencyText.setSpacingAfter(15);
                document.add(emergencyText);
            }

            // 7. Interior & Electronics            // 7. Interior & Electronics
            addSectionHeading(document, "5. INTERIOR AND CABINET DIAGNOSTICS", sectionFont, lightBg, primaryColor);
            PdfPTable intTable = new PdfPTable(3);
            intTable.setWidthPercentage(100);
            try {
                intTable.setWidths(new float[]{3.0f, 2.0f, 1.5f});
            } catch (Exception e) {
                // Ignore
            }
            intTable.setSpacingAfter(15);

            // Table headers
            PdfPCell inH1 = new PdfPCell(new Phrase("Component Name", labelFont)); inH1.setBackgroundColor(lightBg); inH1.setPadding(6); inH1.setBorderColor(borderGray); intTable.addCell(inH1);
            PdfPCell inH2 = new PdfPCell(new Phrase("Status / Value", labelFont)); inH2.setBackgroundColor(lightBg); inH2.setPadding(6); inH2.setBorderColor(borderGray); intTable.addCell(inH2);
            PdfPCell inH3 = new PdfPCell(new Phrase("Diagnostic Photo", labelFont)); inH3.setBackgroundColor(lightBg); inH3.setPadding(6); inH3.setBorderColor(borderGray); intTable.addCell(inH3);

            if (interior != null) {
                addDiagnosticRow(intTable, "Battery Brand / Serial", (interior.getBatteryBrand() != null ? interior.getBatteryBrand() : "N/A") + " / " + (interior.getBatterySerialNumber() != null ? interior.getBatterySerialNumber() : "N/A"), null, labelFont, valueFont, borderGray, lightBg);
                addDiagnosticRow(intTable, "AC Cooling Performance", interior.getAcCooling(), null, labelFont, valueFont, borderGray, lightBg);
                addDiagnosticRow(intTable, "Evaluator Valuation", interior.getEvaluatorValuation() != null ? "₹" + interior.getEvaluatorValuation() : "N/A", null, labelFont, valueFont, borderGray, lightBg);
                
                addDiagnosticRow(intTable, "Right Side Tail Lamp", interior.getRightTailLamp(), images, labelFont, valueFont, borderGray, lightBg);
                addDiagnosticRow(intTable, "Left Side Tail Lamp", interior.getLeftTailLamp(), images, labelFont, valueFont, borderGray, lightBg);
                addDiagnosticRow(intTable, "Right Side Head Light", interior.getRightHeadLamp(), images, labelFont, valueFont, borderGray, lightBg);
                addDiagnosticRow(intTable, "Left Side Head Light", interior.getLeftHeadLamp(), images, labelFont, valueFont, borderGray, lightBg);
                addDiagnosticRow(intTable, "Right Indicator", interior.getIndicators(), images, labelFont, valueFont, borderGray, lightBg);
                addDiagnosticRow(intTable, "Left Indicator", interior.getIndicators(), images, labelFont, valueFont, borderGray, lightBg);
                addDiagnosticRow(intTable, "Boot Floor", interior.getBootFloor(), images, labelFont, valueFont, borderGray, lightBg);
                addDiagnosticRow(intTable, "Dashboard", interior.getDashboard(), images, labelFont, valueFont, borderGray, lightBg);
                addDiagnosticRow(intTable, "Left Side Fog Lamp", interior.getFogLamps(), images, labelFont, valueFont, borderGray, lightBg);
                addDiagnosticRow(intTable, "Right Side Fog Lamp", interior.getFogLamps(), images, labelFont, valueFont, borderGray, lightBg);
                addDiagnosticRow(intTable, "Power Window All Buttons", interior.getPowerWindows(), images, labelFont, valueFont, borderGray, lightBg);
                addDiagnosticRow(intTable, "Music System", interior.getMusicSystem(), images, labelFont, valueFont, borderGray, lightBg);
                addDiagnosticRow(intTable, "Steering Mounted Controls", interior.getSteeringMountedControls(), images, labelFont, valueFont, borderGray, lightBg);
                addDiagnosticRow(intTable, "Wiper Washer Front", interior.getWiper(), images, labelFont, valueFont, borderGray, lightBg);
                addDiagnosticRow(intTable, "Rear Defogger", interior.getRearDefogger(), images, labelFont, valueFont, borderGray, lightBg);
                addDiagnosticRow(intTable, "Rear Wiper Washer", interior.getRearWasher(), images, labelFont, valueFont, borderGray, lightBg);
                addDiagnosticRow(intTable, "Instrument Cluster", interior.getInstrumentCluster(), images, labelFont, valueFont, borderGray, lightBg);
                addDiagnosticRow(intTable, "Infotainment System", interior.getInfotainment(), images, labelFont, valueFont, borderGray, lightBg);
                
                addDiagnosticRow(intTable, "Central Lock", interior.getCentralLock(), images, labelFont, valueFont, borderGray, lightBg);
                addDiagnosticRow(intTable, "Push Start Button", interior.getPushButton(), images, labelFont, valueFont, borderGray, lightBg);
                addDiagnosticRow(intTable, "Sunroof", interior.getSunroof(), images, labelFont, valueFont, borderGray, lightBg);
                addDiagnosticRow(intTable, "All Sensors", interior.getSensors(), images, labelFont, valueFont, borderGray, lightBg);
                addDiagnosticRow(intTable, "Cabinet Remarks", interior.getRemarks(), null, labelFont, valueFont, borderGray, lightBg);
            } else {
                PdfPCell cell = new PdfPCell(new Phrase("No cabinet specifications captured.", valueFont));
                cell.setColspan(3);
                cell.setPadding(8);
                intTable.addCell(cell);
            }
            document.add(intTable);

            // 8. General Remarks & Summary
            addSectionHeading(document, "REMARKS & REPORT STATUS", sectionFont, lightBg, primaryColor);
            PdfPTable remTable = new PdfPTable(2);
            remTable.setWidthPercentage(100);
            remTable.setSpacingAfter(20);

            addStyledTableCell(remTable, "Inspection Status", inspection.getStatus().name(), labelFont, valueFont, lightBg, borderGray);
            addStyledTableCell(remTable, "Lead Evaluator", inspection.getInspector() != null ? inspection.getInspector().getFullName() : "N/A", labelFont, valueFont, lightBg, borderGray);
            addStyledTableCell(remTable, "Rejection Details", inspection.getRejectionReason() != null ? inspection.getRejectionReason() : "None", labelFont, valueFont, lightBg, borderGray);
            addStyledTableCell(remTable, "General Remarks", remarks != null ? remarks.getInspectorRemarks() : "None", labelFont, valueFont, lightBg, borderGray);
            document.add(remTable);

            // 9. Photo Gallery Checklist with Images
            addSectionHeading(document, "INSPECTION PHOTO CAPTURE & GALLERY STATUS", sectionFont, lightBg, primaryColor);

            PdfPTable galleryTable = new PdfPTable(3);
            galleryTable.setWidthPercentage(100);
            try {
                galleryTable.setWidths(new float[]{3.0f, 1.5f, 2.5f});
            } catch (Exception e) {
                // Ignore
            }
            galleryTable.setSpacingAfter(20);

            // Table headers
            PdfPCell h1 = new PdfPCell(new Phrase("Photo Slot Name", labelFont)); h1.setBackgroundColor(lightBg); h1.setPadding(6); h1.setBorderColor(borderGray); galleryTable.addCell(h1);
            PdfPCell h2 = new PdfPCell(new Phrase("Status", labelFont)); h2.setBackgroundColor(lightBg); h2.setPadding(6); h2.setBorderColor(borderGray); galleryTable.addCell(h2);
            PdfPCell h3 = new PdfPCell(new Phrase("Preview Thumbnail", labelFont)); h3.setBackgroundColor(lightBg); h3.setPadding(6); h3.setBorderColor(borderGray); galleryTable.addCell(h3);

            for (PhotoType pt : PhotoType.values()) {
                InspectionImage matchedImage = null;
                if (images != null) {
                    for (InspectionImage img : images) {
                        if (img.getImageCategory() != null && img.getImageUrl() != null) {
                            if (isCategoryMatch(img.getImageCategory(), pt)) {
                                matchedImage = img;
                                break;
                            }
                        }
                    }
                }

                // Slot name
                PdfPCell nameCell = new PdfPCell(new Phrase(pt.getDisplayName(), valueFont));
                nameCell.setPadding(5);
                nameCell.setBorderColor(borderGray);
                galleryTable.addCell(nameCell);

                // Status cell
                String statusText = (matchedImage != null) ? "CAPTURED" : "PENDING";
                PdfPCell statusCell = new PdfPCell(new Phrase(statusText, valueFont));
                statusCell.setPadding(5);
                statusCell.setBorderColor(borderGray);
                if (matchedImage != null) {
                    statusCell.setBackgroundColor(new Color(230, 245, 230)); // Subtle light green
                } else {
                    statusCell.setBackgroundColor(new Color(255, 235, 235)); // Subtle light red
                }
                galleryTable.addCell(statusCell);

                // Image cell
                PdfPCell imgCell = new PdfPCell();
                imgCell.setPadding(4);
                imgCell.setBorderColor(borderGray);
                imgCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                imgCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

                if (matchedImage != null && matchedImage.getImageUrl() != null) {
                    String url = matchedImage.getImageUrl();
                    String filename = url.substring(url.lastIndexOf("/") + 1);
                    java.nio.file.Path imagePath = java.nio.file.Paths.get("uploads/car/images/").resolve(filename);
                    if (java.nio.file.Files.exists(imagePath)) {
                        try {
                            Image pdfImg = Image.getInstance(imagePath.toString());
                            pdfImg.scaleToFit(80, 60);
                            pdfImg.setAlignment(Element.ALIGN_CENTER);
                            imgCell.addElement(pdfImg);
                        } catch (Exception e) {
                            Paragraph noPhoto = new Paragraph("Image Error", valueFont);
                            noPhoto.setAlignment(Element.ALIGN_CENTER);
                            imgCell.addElement(noPhoto);
                        }
                    } else {
                        Paragraph noFile = new Paragraph("File Missing", valueFont);
                        noFile.setAlignment(Element.ALIGN_CENTER);
                        imgCell.addElement(noFile);
                    }
                } else {
                    Paragraph pendingPara = new Paragraph("-", valueFont);
                    pendingPara.setAlignment(Element.ALIGN_CENTER);
                    imgCell.addElement(pendingPara);
                }
                galleryTable.addCell(imgCell);
            }
            document.add(galleryTable);

            // 10. QR Validation Signature
            byte[] qrBytes = generateQrCodeImage("Inspection ID: " + inspection.getId() + 
                    "\nVehicle: " + (v != null ? v.getVehicleNumber() : "N/A") + 
                    "\nStatus: " + inspection.getStatus().name() + 
                    "\nDate: " + inspection.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            if (qrBytes != null) {
                Image qrImage = Image.getInstance(qrBytes);
                qrImage.setAlignment(Element.ALIGN_CENTER);
                qrImage.scaleToFit(100, 100);
                
                Paragraph qrLabel = new Paragraph("Scan to Verify Integrity", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Font.BOLD, darkColor));
                qrLabel.setAlignment(Element.ALIGN_CENTER);
                qrLabel.setSpacingBefore(3);
                
                document.add(qrImage);
                document.add(qrLabel);
            }

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    private void addSectionHeading(Document document, String title, Font font, Color bg, Color border) throws DocumentException {
        PdfPTable secTable = new PdfPTable(1);
        secTable.setWidthPercentage(100);
        secTable.setSpacingBefore(12);
        secTable.setSpacingAfter(8);
        
        PdfPCell cell = new PdfPCell(new Phrase(title, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(border);
        cell.setBorderWidth(1.5f);
        
        secTable.addCell(cell);
        document.add(secTable);
    }

    private void addStyledTableCell(PdfPTable table, String field, String value, Font labelFont, Font valFont, Color bg, Color border) {
        PdfPCell cellLabel = new PdfPCell(new Phrase(field, labelFont));
        cellLabel.setBackgroundColor(bg);
        cellLabel.setPadding(5);
        cellLabel.setBorderColor(border);
        table.addCell(cellLabel);
        
        PdfPCell cellVal = new PdfPCell(new Phrase(value != null ? value : "N/A", valFont));
        cellVal.setPadding(5);
        cellVal.setBorderColor(border);
        table.addCell(cellVal);
    }

    private void addPanelCell(PdfPTable table, String name, String status, String imageUrl, Font labelFont, Font valFont, Color border) {
        PdfPCell labelCell = new PdfPCell(new Phrase(name, valFont));
        labelCell.setPadding(4);
        labelCell.setBorderColor(border);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(labelCell);

        Font statusFont = valFont;
        if ("OK".equalsIgnoreCase(status)) {
            statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, new Color(16, 185, 129));
        } else if ("REPAINTED".equalsIgnoreCase(status) || "CHANGED".equalsIgnoreCase(status)) {
            statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, new Color(255, 140, 0));
        } else if ("DAMAGED".equalsIgnoreCase(status)) {
            statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, new Color(239, 68, 68));
        }

        PdfPCell valCell = new PdfPCell(new Phrase(status, statusFont));
        valCell.setPadding(4);
        valCell.setBorderColor(border);
        valCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(valCell);

        PdfPCell imgCell = new PdfPCell();
        imgCell.setPadding(3);
        imgCell.setBorderColor(border);
        imgCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        imgCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            java.nio.file.Path imagePath = java.nio.file.Paths.get("uploads/car/images/").resolve(filename);
            if (java.nio.file.Files.exists(imagePath)) {
                try {
                    Image pdfImg = Image.getInstance(imagePath.toString());
                    pdfImg.scaleToFit(65, 45);
                    pdfImg.setAlignment(Element.ALIGN_CENTER);
                    imgCell.addElement(pdfImg);
                } catch (Exception e) {
                    Paragraph noPhoto = new Paragraph("-", valFont);
                    noPhoto.setAlignment(Element.ALIGN_CENTER);
                    imgCell.addElement(noPhoto);
                }
            } else {
                Paragraph noPhoto = new Paragraph("-", valFont);
                noPhoto.setAlignment(Element.ALIGN_CENTER);
                imgCell.addElement(noPhoto);
            }
        } else {
            Paragraph noPhoto = new Paragraph("-", valFont);
            noPhoto.setAlignment(Element.ALIGN_CENTER);
            imgCell.addElement(noPhoto);
        }
        table.addCell(imgCell);
    }

    private void addTyreRow(PdfPTable table, String pos, String brand, String info, Font valFont) {
        table.addCell(new Phrase(pos, valFont));
        table.addCell(new Phrase(brand != null ? brand : "N/A", valFont));
        table.addCell(new Phrase(info != null ? info : "N/A", valFont));
    }

    private void addRatingCell(PdfPTable table, String label, Double rating, Color gold, Color border) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(6);
        cell.setBorderColor(border);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        
        Paragraph labelPara = new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Font.BOLD, new Color(100, 100, 110)));
        labelPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(labelPara);
        
        String valStr = rating != null ? String.format("%.1f", rating) + " / 5.0" : "N/A";
        Paragraph valPara = new Paragraph(valStr, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.BOLD, gold));
        valPara.setAlignment(Element.ALIGN_CENTER);
        valPara.setSpacingBefore(3);
        cell.addElement(valPara);
        
        table.addCell(cell);
    }

    private String getBooleanText(Boolean value) {
        return Boolean.TRUE.equals(value) ? "YES" : "NO";
    }

    private byte[] generateQrCodeImage(String text) {
        try {
            com.google.zxing.qrcode.QRCodeWriter qrCodeWriter = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(text, com.google.zxing.BarcodeFormat.QR_CODE, 100, 100);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            com.google.zxing.client.j2se.MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isCategoryMatch(String cat, PhotoType pt) {
        if (cat == null) return false;
        String cleanCat = cat.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        String cleanPt = pt.name().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        
        if (cleanCat.equals(cleanPt)) return true;
        
        if (pt == PhotoType.FRONT_VIEW && (cleanCat.equals("FRONT") || cleanCat.equals("FRONTVIEW"))) return true;
        if (pt == PhotoType.REAR_VIEW && (cleanCat.equals("REAR") || cleanCat.equals("REARVIEW") || cleanCat.equals("BACK"))) return true;
        if (pt == PhotoType.LEFT_FRONT_VIEW && (cleanCat.equals("LEFT") || cleanCat.equals("LEFTFRONT") || cleanCat.equals("LEFTFRONTVIEW"))) return true;
        if (pt == PhotoType.RIGHT_FRONT_VIEW && (cleanCat.equals("RIGHT") || cleanCat.equals("RIGHTFRONT") || cleanCat.equals("RIGHTFRONTVIEW"))) return true;
        if (pt == PhotoType.ROOF_VIEW && (cleanCat.equals("ROOF") || cleanCat.equals("ROOFVIEW"))) return true;
        if (pt == PhotoType.ENGINE_IMAGE && (cleanCat.equals("ENGINE") || cleanCat.equals("ENGINEIMAGE"))) return true;
        if (pt == PhotoType.BATTERY_IMAGE && (cleanCat.equals("BATTERY") || cleanCat.equals("BATTERYIMAGE"))) return true;
        if (pt == PhotoType.ODOMETER_IMAGE && (cleanCat.equals("ODOMETER") || cleanCat.equals("ODOMETERIMAGE"))) return true;
        if (pt == PhotoType.DASHBOARD_IMAGE && (cleanCat.equals("DASHBOARD") || cleanCat.equals("DASHBOARDIMAGE") || cleanCat.equals("INTERIOR"))) return true;
        if (pt == PhotoType.AC_CONTROL_IMAGE && (cleanCat.equals("AC") || cleanCat.equals("ACCONTROL") || cleanCat.equals("ACCONTROLIMAGE"))) return true;
        if (pt == PhotoType.INSTRUMENT_CLUSTER_IMAGE && (cleanCat.equals("CLUSTER") || cleanCat.equals("INSTRUMENTCLUSTER") || cleanCat.equals("INSTRUMENTCLUSTERIMAGE"))) return true;
        if (pt == PhotoType.MUSIC_SYSTEM_IMAGE && (cleanCat.equals("MUSIC") || cleanCat.equals("MUSICSYSTEM") || cleanCat.equals("MUSICSYSTEMIMAGE"))) return true;
        
        return false;
    }

    private void addDiagnosticRow(PdfPTable table, String name, String value, List<InspectionImage> images, Font labelFont, Font valFont, Color border, Color lightBg) {
        // 1. Label cell
        PdfPCell labelCell = new PdfPCell(new Phrase(name, labelFont));
        labelCell.setPadding(5);
        labelCell.setBorderColor(border);
        labelCell.setBackgroundColor(lightBg);
        table.addCell(labelCell);

        // 2. Status cell with color coding
        boolean isOk = value != null && (value.toUpperCase().contains("OK") || value.toUpperCase().contains("WORKING") || value.toUpperCase().contains("YES"));
        Font statusFont = valFont;
        if (value != null && !value.isEmpty()) {
            if (isOk) {
                statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, new Color(16, 185, 129)); // Green
            } else {
                statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, new Color(239, 68, 68)); // Red
            }
        }
        
        PdfPCell valCell = new PdfPCell(new Phrase(value != null ? value : "N/A", statusFont));
        valCell.setPadding(5);
        valCell.setBorderColor(border);
        table.addCell(valCell);

        // 3. Photo cell
        PdfPCell photoCell = new PdfPCell();
        photoCell.setPadding(4);
        photoCell.setBorderColor(border);
        photoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        photoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        InspectionImage matched = null;
        if (images != null && value != null) {
            for (InspectionImage img : images) {
                if (img.getImageCategory() != null && img.getImageUrl() != null) {
                    if (img.getImageCategory().equalsIgnoreCase(name)) {
                        matched = img;
                        break;
                    }
                }
            }
        }

        if (matched != null) {
            String url = matched.getImageUrl();
            String filename = url.substring(url.lastIndexOf("/") + 1);
            java.nio.file.Path imagePath = java.nio.file.Paths.get("uploads/car/images/").resolve(filename);
            if (java.nio.file.Files.exists(imagePath)) {
                try {
                    Image pdfImg = Image.getInstance(imagePath.toString());
                    pdfImg.scaleToFit(50, 38);
                    pdfImg.setAlignment(Element.ALIGN_CENTER);
                    photoCell.addElement(pdfImg);
                } catch (Exception e) {
                    photoCell.addElement(new Phrase("Error", valFont));
                }
            } else {
                photoCell.addElement(new Phrase("Missing", valFont));
            }
        } else {
            photoCell.addElement(new Phrase("-", valFont));
        }
        table.addCell(photoCell);
    }
}
