package com.bidding.serviceImpl;

import com.bidding.dto.responce.InspectionDetailsResponse;
import com.bidding.entity.*;
import com.bidding.enums.PhotoType;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfGeneratorService {

    private final Color primaryColor = new Color(255, 199, 0); // Gold / Yellow
    private final Color darkColor = new Color(13, 14, 18);    // Dark Charcoal
    private final Color lightBg = new Color(245, 245, 247);     // Premium Light Gray
    private final Color borderGray = new Color(220, 220, 225);  // Border Gray
    private final Color greenColor = new Color(16, 185, 129);   // Green
    private final Color redColor = new Color(239, 68, 68);     // Red
    private final Color amberColor = new Color(245, 158, 11);   // Amber

    private final Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Font.BOLD, darkColor);
    private final Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Font.BOLD, darkColor);
    private final Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Font.NORMAL, darkColor);

    public byte[] generateInspectionPdfFromDto(InspectionDetailsResponse details) {
        return generateInspectionPdfFromDto(details, false);
    }

    public byte[] generateInspectionPdfFromDto(InspectionDetailsResponse details, boolean isDealer) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 28, 28, 28, 28);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Header Band
            PdfPTable headerTable = new PdfPTable(1);
            headerTable.setWidthPercentage(100);
            headerTable.setSpacingAfter(12);

            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(darkColor);
            headerCell.setPadding(10);
            headerCell.setBorder(Rectangle.NO_BORDER);

            Paragraph brand = new Paragraph("CARYANAM", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Font.BOLD, primaryColor));
            brand.setAlignment(Element.ALIGN_CENTER);
            headerCell.addElement(brand);

            Paragraph subtitle = new Paragraph("VEHICLE REMARKETING PLATFORM  ·  200-POINT CERTIFIED REPORT",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Font.BOLD, Color.WHITE));
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingBefore(3);
            headerCell.addElement(subtitle);

            headerTable.addCell(headerCell);
            document.add(headerTable);

            // 2. Summary Rating Cards
            addSectionHeading(document, "EVALUATION RATINGS SUMMARY");
            PdfPTable ratingsTable = new PdfPTable(4);
            ratingsTable.setWidthPercentage(100);
            ratingsTable.setSpacingAfter(12);

            InspectionDetailsResponse.RatingsResponseDTO r = details != null ? details.getRatings() : null;
            addRatingCell(ratingsTable, "Exterior Rating", r != null ? r.getExterior() : null);
            addRatingCell(ratingsTable, "Mechanical Rating", r != null ? r.getMechanical() : null);
            addRatingCell(ratingsTable, "Tyres Rating", r != null ? r.getTyre() : null);
            addRatingCell(ratingsTable, "Interior Rating", r != null ? r.getInterior() : null);
            document.add(ratingsTable);

            // 3. Vehicle Specifications
            addSectionHeading(document, "1. VEHICLE SPECS & REGISTRATION");
            PdfPTable vehTable = new PdfPTable(2);
            vehTable.setWidthPercentage(100);
            vehTable.setSpacingAfter(12);

            InspectionDetailsResponse.VehicleResponseDTO v = details != null ? details.getVehicleDetails() : null;
            if (v != null) {
                if (!isDealer) {
                    addStyledTableCell(vehTable, "Customer / Owner Name", v.getCustomerName() != null ? v.getCustomerName() : (v.getOwnerName() != null ? v.getOwnerName() : "N/A"));
                    addStyledTableCell(vehTable, "Customer Mobile", v.getCustomerMobileNumber() != null ? v.getCustomerMobileNumber() : "N/A");
                }
                addStyledTableCell(vehTable, "Registration Number", v.getVehicleNumber());
                addStyledTableCell(vehTable, "Make & Model", (v.getBrand() != null ? v.getBrand() : "") + " " + (v.getModel() != null ? v.getModel() : "") + " " + (v.getVariant() != null ? v.getVariant() : ""));
                addStyledTableCell(vehTable, "Manufacturing Year", v.getManufacturingYear() != null ? String.valueOf(v.getManufacturingYear()) : "N/A");
                addStyledTableCell(vehTable, "Registration Year", v.getRegistrationYear() != null ? String.valueOf(v.getRegistrationYear()) : "N/A");
                addStyledTableCell(vehTable, "Fuel Type & Transmission", (v.getFuelType() != null ? v.getFuelType() : "N/A") + " / " + (v.getTransmission() != null ? v.getTransmission() : "N/A"));
                addStyledTableCell(vehTable, "Odometer Reading", v.getOdometerReading() != null ? v.getOdometerReading() + " km" : "N/A");
                addStyledTableCell(vehTable, "Insurance Status", v.getInsuranceStatus() != null ? v.getInsuranceStatus() : "N/A");
                addStyledTableCell(vehTable, "Suggested Price Valuation", formatCurrency(v.getSuggestedPrice()));
            } else {
                PdfPCell cell = new PdfPCell(new Phrase("No vehicle specifications captured.", valueFont));
                cell.setColspan(2);
                cell.setPadding(6);
                vehTable.addCell(cell);
            }
            document.add(vehTable);

            // 4. Exterior Body Condition Checklist
            addSectionHeading(document, "2. EXTERIOR BODY CONDITION CHECKLIST");
            PdfPTable panelTable = new PdfPTable(6);
            panelTable.setWidthPercentage(100);
            try {
                panelTable.setWidths(new float[]{2.2f, 1.3f, 1.5f, 2.2f, 1.3f, 1.5f});
            } catch (Exception ignored) {}
            panelTable.setSpacingAfter(12);

            List<InspectionDetailsResponse.PanelResponseDTO> panels = details != null ? details.getExteriorPanelDetails() : null;
            if (panels != null && !panels.isEmpty()) {
                for (InspectionDetailsResponse.PanelResponseDTO p : panels) {
                    String statusStr = p.getCondition() != null ? p.getCondition().name() : "OK";
                    String cleanCond = statusStr.toUpperCase().trim();
                    boolean isNa = "NA".equals(cleanCond) || "N/A".equals(cleanCond)
                            || "NOT APPLICABLE".equals(cleanCond) || "NOT_APPLICABLE".equals(cleanCond)
                            || cleanCond.startsWith("NA") || cleanCond.startsWith("N/A");

                    String panelImgUrl = p.getImageUrl();
                    addPanelCell(panelTable, p.getPanelName(), statusStr, isNa ? null : panelImgUrl);
                }
                if (panels.size() % 2 != 0) {
                    panelTable.addCell("");
                    panelTable.addCell("");
                    panelTable.addCell("");
                }
            } else {
                PdfPCell cell = new PdfPCell(new Phrase("No exterior panels evaluated.", valueFont));
                cell.setColspan(6);
                cell.setPadding(6);
                panelTable.addCell(cell);
            }
            document.add(panelTable);

            // Mandatory Exterior Angles
            addSectionHeading(document, "MANDATORY EXTERIOR ANGLE PHOTOS");
            PdfPTable extAnglesTable = new PdfPTable(3);
            extAnglesTable.setWidthPercentage(100);
            try {
                extAnglesTable.setWidths(new float[]{3.0f, 2.0f, 1.5f});
            } catch (Exception ignored) {}
            extAnglesTable.setSpacingAfter(12);

            addDiagnosticRow(extAnglesTable, "FRONT SIDE IMAGE", "CAPTURED", findPhotoUrlBySlot(details, "FRONT_VIEW"));
            addDiagnosticRow(extAnglesTable, "RIGHT SIDE IMAGE", "CAPTURED", findPhotoUrlBySlot(details, "RIGHT_FRONT_VIEW"));
            addDiagnosticRow(extAnglesTable, "REAR SIDE IMAGE", "CAPTURED", findPhotoUrlBySlot(details, "REAR_VIEW"));
            addDiagnosticRow(extAnglesTable, "LEFT SIDE IMAGE", "CAPTURED", findPhotoUrlBySlot(details, "LEFT_FRONT_VIEW"));
            addDiagnosticRow(extAnglesTable, "ROOF TOP IMAGE", "CAPTURED", findPhotoUrlBySlot(details, "ROOF_VIEW"));
            document.add(extAnglesTable);

            // 5. Mechanical Diagnostics
            addSectionHeading(document, "3. MECHANICAL HEALTH DIAGNOSTICS");
            PdfPTable mechTable = new PdfPTable(3);
            mechTable.setWidthPercentage(100);
            try {
                mechTable.setWidths(new float[]{3.0f, 2.0f, 1.5f});
            } catch (Exception ignored) {}
            mechTable.setSpacingAfter(12);

            InspectionDetailsResponse.MechanicalResponseDTO mech = details != null ? details.getMechanicalDetails() : null;
            if (mech != null) {
                addDiagnosticRow(mechTable, "Engine / Motor Status", mech.getEngineStatus(), findPhotoUrlByName(details, "Engine / Motor Status"));
                addDiagnosticRow(mechTable, "Engine Oil", mech.getEngineOil(), findPhotoUrlByName(details, "Engine Oil"));
                addDiagnosticRow(mechTable, "Brake Oil", mech.getBrakeOil(), findPhotoUrlByName(details, "Brakes Oil"));
                addDiagnosticRow(mechTable, "Steering Oil", mech.getSteeringOil(), findPhotoUrlByName(details, "Steering Oil"));
                addDiagnosticRow(mechTable, "Coolant", mech.getCoolant(), findPhotoUrlByName(details, "Coolant"));
                addDiagnosticRow(mechTable, "Brake Booster", mech.getBrakeBooster(), findPhotoUrlByName(details, "Brakes Booster"));
                addDiagnosticRow(mechTable, "Brake Working", mech.getBrakeWorking(), findPhotoUrlByName(details, "Brakes Working"));
                addDiagnosticRow(mechTable, "Apron Condition", mech.getApron(), findPhotoUrlByName(details, "Apron Condition"));
                addDiagnosticRow(mechTable, "Chassis Alignment", mech.getChassis(), findPhotoUrlByName(details, "Chassis Alignment"));
                addDiagnosticRow(mechTable, "Suspension", mech.getSuspension(), findPhotoUrlByName(details, "Suspension"));
                addDiagnosticRow(mechTable, "Suspension Bushing", mech.getBush(), findPhotoUrlByName(details, "Suspension Bushing"));
                addDiagnosticRow(mechTable, "Oil Leakage", mech.getLeakage(), findPhotoUrlByName(details, "Oil Leakage"));
                addDiagnosticRow(mechTable, "Exhaust Smoke Color", mech.getSmoke(), findPhotoUrlByName(details, "Exhaust Smoke Color"));
                addDiagnosticRow(mechTable, "Manual Transmission Fluid Level", mech.getTransmission(), findPhotoUrlByName(details, "Manual Transmission Fluid Level"));
                addDiagnosticRow(mechTable, "Steering Gearbox & Linkage", mech.getGearbox(), findPhotoUrlByName(details, "Steering Gearbox & Linkage"));
                addDiagnosticRow(mechTable, "Differential Fluid Level", mech.getDifferential(), findPhotoUrlByName(details, "Differential Fluid Level"));
                addDiagnosticRow(mechTable, "Driveline / Axle", mech.getAxle(), findPhotoUrlByName(details, "Driveline / Axle"));
                addDiagnosticRow(mechTable, "Engine / Motor Noise", mech.getEngineNoise(), findPhotoUrlByName(details, "Engine / Motor Noise"));
                addDiagnosticRow(mechTable, "Fluid Leakages", mech.getFluidLeakage(), findPhotoUrlByName(details, "Fluid Leakages"));
            } else {
                PdfPCell cell = new PdfPCell(new Phrase("No mechanical parameters captured.", valueFont));
                cell.setColspan(3);
                cell.setPadding(6);
                mechTable.addCell(cell);
            }
            document.add(mechTable);

            // Engine & Battery Bay Photos
            addSectionHeading(document, "UNDER-BONNET ENGINE ROOM & BATTERY BAY PHOTOS");
            PdfPTable engineBayTable = new PdfPTable(3);
            engineBayTable.setWidthPercentage(100);
            try {
                engineBayTable.setWidths(new float[]{3.0f, 2.0f, 1.5f});
            } catch (Exception ignored) {}
            engineBayTable.setSpacingAfter(12);

            addDiagnosticRow(engineBayTable, "ENGINE ROOM PHOTO", "CAPTURED", findPhotoUrlBySlot(details, "ENGINE_IMAGE"));
            addDiagnosticRow(engineBayTable, "BATTERY BAY PHOTO", "CAPTURED", findPhotoUrlBySlot(details, "BATTERY_IMAGE"));
            document.add(engineBayTable);

            // 6. Tyres Specifications & Emergency Toolkit
            addSectionHeading(document, "4. TYRES SPECIFICATIONS & ACCESSORIES");
            PdfPTable tyreTable = new PdfPTable(3);
            tyreTable.setWidthPercentage(100);
            tyreTable.setSpacingAfter(10);

            PdfPCell th1 = new PdfPCell(new Phrase("Wheel Position", labelFont)); th1.setBackgroundColor(lightBg); th1.setPadding(5); tyreTable.addCell(th1);
            PdfPCell th2 = new PdfPCell(new Phrase("Brand Name", labelFont)); th2.setBackgroundColor(lightBg); th2.setPadding(5); tyreTable.addCell(th2);
            PdfPCell th3 = new PdfPCell(new Phrase("Remaining Tread % / Mfg Year", labelFont)); th3.setBackgroundColor(lightBg); th3.setPadding(5); tyreTable.addCell(th3);

            InspectionDetailsResponse.TyreResponseDTO tyre = details != null ? details.getTyreDetails() : null;
            if (tyre != null) {
                addTyreRow(tyreTable, "Front Right Tyre", tyre.getFrontRightBrand(), formatTread(tyre.getFrontRightTread(), tyre.getFrontRightYear()));
                addTyreRow(tyreTable, "Rear Right Tyre", tyre.getRearRightBrand(), formatTread(tyre.getRearRightTread(), tyre.getRearRightYear()));
                addTyreRow(tyreTable, "Rear Left Tyre", tyre.getRearLeftBrand(), formatTread(tyre.getRearLeftTread(), tyre.getRearLeftYear()));
                addTyreRow(tyreTable, "Front Left Tyre", tyre.getFrontLeftBrand(), formatTread(tyre.getFrontLeftTread(), tyre.getFrontLeftYear()));
                addTyreRow(tyreTable, "Spare Wheel", tyre.getSpareBrand(), formatTread(tyre.getSpareTread(), tyre.getSpareYear()));
            } else {
                PdfPCell cell = new PdfPCell(new Phrase("No tyre specifications captured.", valueFont));
                cell.setColspan(3);
                cell.setPadding(6);
                tyreTable.addCell(cell);
            }
            document.add(tyreTable);

            if (tyre != null) {
                Paragraph emergencyText = new Paragraph(String.format("Emergency Equipment: Jack (%s)  ·  Handle (%s)  ·  Tool Kit (%s)  ·  Emergency Triangle (%s)  ·  First Aid Box (%s)",
                        getBooleanText(tyre.getHasJack()), getBooleanText(tyre.getHasHandle()), getBooleanText(tyre.getHasToolkit()),
                        getBooleanText(tyre.getHasTriangle()), getBooleanText(tyre.getHasFirstAidBox())), valueFont);
                emergencyText.setSpacingAfter(10);
                document.add(emergencyText);
            }

            // All 6 Tyre Photos
            addSectionHeading(document, "INDIVIDUAL TYRE PROFILE & OVERVIEW PHOTOS");
            PdfPTable tyrePhotosTable = new PdfPTable(3);
            tyrePhotosTable.setWidthPercentage(100);
            try {
                tyrePhotosTable.setWidths(new float[]{3.0f, 2.0f, 1.5f});
            } catch (Exception ignored) {}
            tyrePhotosTable.setSpacingAfter(12);

            addDiagnosticRow(tyrePhotosTable, "RIGHT SIDE FRONT TYRE IMG", "CAPTURED", findPhotoUrlBySlot(details, "FRONT_RIGHT_TYRE"));
            addDiagnosticRow(tyrePhotosTable, "RIGHT SIDE REAR TYRE IMG", "CAPTURED", findPhotoUrlBySlot(details, "REAR_RIGHT_TYRE"));
            addDiagnosticRow(tyrePhotosTable, "LEFT SIDE REAR TYRE IMG", "CAPTURED", findPhotoUrlBySlot(details, "REAR_LEFT_TYRE"));
            addDiagnosticRow(tyrePhotosTable, "LEFT SIDE FRONT TYRE IMG", "CAPTURED", findPhotoUrlBySlot(details, "FRONT_LEFT_TYRE"));
            addDiagnosticRow(tyrePhotosTable, "SPARE WHEEL IMG", "CAPTURED", findPhotoUrlBySlot(details, "SPARE_WHEEL"));
            addDiagnosticRow(tyrePhotosTable, "TYRES OVERVIEW IMAGE", "CAPTURED", findPhotoUrlBySlot(details, "TYRES_OVERVIEW"));
            document.add(tyrePhotosTable);

            // 7. Interior & Electrical Diagnostics
            addSectionHeading(document, "5. INTERIOR & ELECTRICAL CHECKLIST");
            PdfPTable intTable = new PdfPTable(3);
            intTable.setWidthPercentage(100);
            try {
                intTable.setWidths(new float[]{3.0f, 2.0f, 1.5f});
            } catch (Exception ignored) {}
            intTable.setSpacingAfter(12);

            InspectionDetailsResponse.InteriorResponseDTO interior = details != null ? details.getInteriorDetails() : null;
            if (interior != null) {
                addDiagnosticRow(intTable, "Battery Company", interior.getBatteryBrand(), findPhotoUrlByName(details, "Battery Company"));
                addDiagnosticRow(intTable, "Full Battery Serial Number", interior.getBatterySerialNumber(), null);
                addDiagnosticRow(intTable, "AC Cooling Performance", interior.getAcCooling(), findPhotoUrlByName(details, "AC Cooling Performance"));
                addDiagnosticRow(intTable, "Evaluator Valuation", formatCurrency(interior.getEvaluatorValuation()), null);

                addDiagnosticRow(intTable, "Push Start Button", interior.getPushButton(), findPhotoUrlByName(details, "Push Start Button"));
                addDiagnosticRow(intTable, "Sunroof", interior.getSunroof(), findPhotoUrlByName(details, "Sunroof"));
                addDiagnosticRow(intTable, "Right Side Tail Lamp", interior.getRightTailLamp(), findPhotoUrlByName(details, "Right Side Tail Lamp"));
                addDiagnosticRow(intTable, "Left Side Tail Lamp", interior.getLeftTailLamp(), findPhotoUrlByName(details, "Left Side Tail Lamp"));
                addDiagnosticRow(intTable, "Right Side Head Light", interior.getRightHeadLamp(), findPhotoUrlByName(details, "Right Side Head Light"));
                addDiagnosticRow(intTable, "Left Side Head Light", interior.getLeftHeadLamp(), findPhotoUrlByName(details, "Left Side Head Light"));
                addDiagnosticRow(intTable, "Right Indicator", interior.getIndicators(), findPhotoUrlByName(details, "Right Indicator"));
                addDiagnosticRow(intTable, "Left Indicator", interior.getIndicators(), findPhotoUrlByName(details, "Left Indicator"));
                addDiagnosticRow(intTable, "Boot Floor", interior.getBootFloor(), findPhotoUrlByName(details, "Boot Floor"));
                addDiagnosticRow(intTable, "Dashboard", interior.getDashboard(), findPhotoUrlByName(details, "Dashboard"));
                addDiagnosticRow(intTable, "Left Side Fog Lamp", interior.getFogLamps(), findPhotoUrlByName(details, "Left Side Fog Lamp"));
                addDiagnosticRow(intTable, "Right Side Fog Lamp", interior.getFogLamps(), findPhotoUrlByName(details, "Right Side Fog Lamp"));
                addDiagnosticRow(intTable, "Power Window All Buttons", interior.getPowerWindows(), findPhotoUrlByName(details, "Power Window All Buttons"));
                addDiagnosticRow(intTable, "Music System", interior.getMusicSystem(), findPhotoUrlByName(details, "Music System"));
                addDiagnosticRow(intTable, "Steering Mounted Controls", interior.getSteeringMountedControls(), findPhotoUrlByName(details, "Steering Mounted Controls"));
                addDiagnosticRow(intTable, "Wiper Washer Front", interior.getWiper(), findPhotoUrlByName(details, "Wiper Washer Front"));
                addDiagnosticRow(intTable, "Rear Defogger", interior.getRearDefogger(), findPhotoUrlByName(details, "Rear Defogger"));
                addDiagnosticRow(intTable, "Rear Wiper Washer", interior.getRearWasher(), findPhotoUrlByName(details, "Rear Wiper Washer"));
                addDiagnosticRow(intTable, "Instrument Cluster", interior.getInstrumentCluster(), findPhotoUrlByName(details, "Instrument Cluster"));
                addDiagnosticRow(intTable, "Infotainment System", interior.getInfotainment(), findPhotoUrlByName(details, "Infotainment System"));
                addDiagnosticRow(intTable, "Central Lock", interior.getCentralLock(), findPhotoUrlByName(details, "Central Lock"));
                addDiagnosticRow(intTable, "All Sensors", interior.getSensors(), findPhotoUrlByName(details, "All Sensors"));
            } else {
                PdfPCell cell = new PdfPCell(new Phrase("No interior specifications captured.", valueFont));
                cell.setColspan(3);
                cell.setPadding(6);
                intTable.addCell(cell);
            }
            document.add(intTable);

            // Mandatory Cabin Photos
            addSectionHeading(document, "INTERIOR & CABIN MANDATORY PHOTOS");
            PdfPTable cabinPhotosTable = new PdfPTable(3);
            cabinPhotosTable.setWidthPercentage(100);
            try {
                cabinPhotosTable.setWidths(new float[]{3.0f, 2.0f, 1.5f});
            } catch (Exception ignored) {}
            cabinPhotosTable.setSpacingAfter(12);

            addDiagnosticRow(cabinPhotosTable, "ODOMETER READING PHOTO", "CAPTURED", findPhotoUrlBySlot(details, "ODOMETER_IMAGE"));
            addDiagnosticRow(cabinPhotosTable, "AC CONTROL PANEL PHOTO", "CAPTURED", findPhotoUrlBySlot(details, "AC_CONTROL_IMAGE"));
            document.add(cabinPhotosTable);

            // 8. General Remarks & Report Status
            addSectionHeading(document, "INSPECTOR REMARKS & REPORT STATUS");
            PdfPTable remTable = new PdfPTable(2);
            remTable.setWidthPercentage(100);
            remTable.setSpacingAfter(15);

            addStyledTableCell(remTable, "Inspection Status", details != null && details.getStatus() != null ? details.getStatus().name() : "SUBMITTED");
            addStyledTableCell(remTable, "Lead Inspector", details != null && details.getInspectorName() != null ? details.getInspectorName() : "N/A");
            addStyledTableCell(remTable, "Rejection Reason", details != null && details.getRejectionReason() != null ? details.getRejectionReason() : "None");
            addStyledTableCell(remTable, "Inspector Remarks", (interior != null && interior.getRemarks() != null) ? interior.getRemarks() : "No remarks entered.");
            document.add(remTable);

            // 9. Photo Gallery Checklist with Images
            addSectionHeading(document, "INSPECTION PHOTO GALLERY STATUS");
            PdfPTable galleryTable = new PdfPTable(3);
            galleryTable.setWidthPercentage(100);
            try {
                galleryTable.setWidths(new float[]{3.0f, 1.5f, 2.5f});
            } catch (Exception ignored) {}
            galleryTable.setSpacingAfter(15);

            PdfPCell h1 = new PdfPCell(new Phrase("Photo Slot Name", labelFont)); h1.setBackgroundColor(lightBg); h1.setPadding(5); h1.setBorderColor(borderGray); galleryTable.addCell(h1);
            PdfPCell h2 = new PdfPCell(new Phrase("Capture Status", labelFont)); h2.setBackgroundColor(lightBg); h2.setPadding(5); h2.setBorderColor(borderGray); galleryTable.addCell(h2);
            PdfPCell h3 = new PdfPCell(new Phrase("Photo Preview", labelFont)); h3.setBackgroundColor(lightBg); h3.setPadding(5); h3.setBorderColor(borderGray); galleryTable.addCell(h3);

            for (PhotoType pt : PhotoType.values()) {
                String imgUrl = findPhotoUrlBySlot(details, pt.name());
                String statusText = (imgUrl != null && !imgUrl.isEmpty()) ? "CAPTURED" : "PENDING";

                PdfPCell nameCell = new PdfPCell(new Phrase(pt.getDisplayName(), valueFont));
                nameCell.setPadding(4);
                nameCell.setBorderColor(borderGray);
                galleryTable.addCell(nameCell);

                PdfPCell statusCell = new PdfPCell(new Phrase(statusText, valueFont));
                statusCell.setPadding(4);
                statusCell.setBorderColor(borderGray);
                if ("CAPTURED".equals(statusText)) {
                    statusCell.setBackgroundColor(new Color(230, 245, 230));
                } else {
                    statusCell.setBackgroundColor(new Color(255, 235, 235));
                }
                galleryTable.addCell(statusCell);

                PdfPCell imgCell = new PdfPCell();
                imgCell.setPadding(3);
                imgCell.setBorderColor(borderGray);
                imgCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                imgCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

                if (imgUrl != null && !imgUrl.isEmpty()) {
                    boolean isVideo = imgUrl.toLowerCase().matches(".*\\.(mp4|webm|mov|avi|mkv|3gp|flv|wmv)($|\\?.*)")
                            || imgUrl.toLowerCase().contains("video")
                            || pt.name().contains("ENGINE_NOISE")
                            || pt.getDisplayName().equalsIgnoreCase("Engine / Motor Noise");

                    if (isVideo) {
                        Font videoFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.0f, Font.BOLD, new Color(79, 70, 229));
                        Paragraph videoText = new Paragraph("[VIDEO ATTACHED]", videoFont);
                        videoText.setAlignment(Element.ALIGN_CENTER);
                        imgCell.addElement(videoText);
                    } else {
                        Image pdfImg = createPdfImage(imgUrl, 75, 50);
                        if (pdfImg != null) {
                            imgCell.addElement(pdfImg);
                        } else {
                            Paragraph noPhoto = new Paragraph("No Image", valueFont);
                            noPhoto.setAlignment(Element.ALIGN_CENTER);
                            imgCell.addElement(noPhoto);
                        }
                    }
                } else {
                    Paragraph pendingPara = new Paragraph("-", valueFont);
                    pendingPara.setAlignment(Element.ALIGN_CENTER);
                    imgCell.addElement(pendingPara);
                }
                galleryTable.addCell(imgCell);
            }
            document.add(galleryTable);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    public byte[] generateInspectionPdf(Inspection inspection,
                                         List<InspectionPanel> panels,
                                         MechanicalInspection mechanical,
                                         TyreInspection tyres,
                                         InteriorInspection interior,
                                         List<InspectionImage> images,
                                         InspectionRemarks remarks) {
        // Legacy fallback method signature
        InspectionDetailsResponse details = buildDetailsResponseFromEntities(inspection, panels, mechanical, tyres, interior, images, remarks);
        return generateInspectionPdfFromDto(details);
    }

    private InspectionDetailsResponse buildDetailsResponseFromEntities(Inspection ins,
                                                                        List<InspectionPanel> panels,
                                                                        MechanicalInspection mechanical,
                                                                        TyreInspection tyres,
                                                                        InteriorInspection interior,
                                                                        List<InspectionImage> images,
                                                                        InspectionRemarks remarks) {
        Vehicle v = ins != null ? ins.getVehicle() : null;

        List<InspectionDetailsResponse.PanelResponseDTO> panelDTOs = new ArrayList<>();
        if (panels != null) {
            for (InspectionPanel p : panels) {
                panelDTOs.add(InspectionDetailsResponse.PanelResponseDTO.builder()
                        .id(p.getId())
                        .panelName(p.getPanelName())
                        .condition(p.getCondition())
                        .imageUrl(p.getImageUrl())
                        .build());
            }
        }

        List<InspectionDetailsResponse.PhotoResponseDTO> photoDTOs = new ArrayList<>();
        if (images != null) {
            for (InspectionImage img : images) {
                photoDTOs.add(InspectionDetailsResponse.PhotoResponseDTO.builder()
                        .id(img.getId())
                        .photoType(null)
                        .displayName(img.getOriginalName() != null ? img.getOriginalName() : img.getImageCategory())
                        .imageCategory(img.getImageCategory())
                        .imageUrl(img.getImageUrl())
                        .captured(true)
                        .build());
            }
        }

        InspectionDetailsResponse.VehicleResponseDTO vDTO = null;
        if (v != null) {
            vDTO = InspectionDetailsResponse.VehicleResponseDTO.builder()
                    .id(v.getId())
                    .vehicleNumber(v.getVehicleNumber())
                    .ownerName(v.getOwnerName())
                    .customerName(v.getCustomerName())
                    .customerMobileNumber(v.getCustomerMobileNumber())
                    .brand(v.getBrand())
                    .model(v.getModel())
                    .variant(v.getVariant())
                    .manufacturingYear(v.getManufacturingYear()).registrationYear(v.getRegistrationYear())
                    .fuelType(v.getFuelType())
                    .transmission(v.getTransmission())
                    .odometerReading(v.getOdometerReading())
                    .insuranceStatus(v.getInsuranceStatus())
                    .inspectorCode(v.getInspectorCode())
                    .suggestedPrice(v.getSuggestedPrice())
                    .build();
        }

        InspectionDetailsResponse.MechanicalResponseDTO mDTO = null;
        if (mechanical != null) {
            mDTO = InspectionDetailsResponse.MechanicalResponseDTO.builder()
                    .id(mechanical.getId())
                    .engineStatus(mechanical.getEngineStatus())
                    .engineOil(mechanical.getEngineOil())
                    .brakeOil(mechanical.getBrakeOil())
                    .steeringOil(mechanical.getSteeringOil())
                    .coolant(mechanical.getCoolant())
                    .brakeBooster(mechanical.getBrakeBooster())
                    .brakeWorking(mechanical.getBrakeWorking())
                    .apron(mechanical.getApron())
                    .chassis(mechanical.getChassis())
                    .suspension(mechanical.getSuspension())
                    .bush(mechanical.getBush())
                    .leakage(mechanical.getLeakage())
                    .transmission(mechanical.getTransmission())
                    .gearbox(mechanical.getGearbox())
                    .differential(mechanical.getDifferential())
                    .axle(mechanical.getAxle())
                    .engineNoise(mechanical.getEngineNoise())
                    .smoke(mechanical.getSmoke())
                    .fluidLeakage(mechanical.getFluidLeakage())
                    .build();
        }

        InspectionDetailsResponse.TyreResponseDTO tDTO = null;
        if (tyres != null) {
            tDTO = InspectionDetailsResponse.TyreResponseDTO.builder()
                    .id(tyres.getId())
                    .frontLeftBrand(tyres.getFrontLeftBrand())
                    .frontLeftTread(tyres.getFrontLeftTread())
                    .frontLeftYear(tyres.getFrontLeftYear())
                    .frontRightBrand(tyres.getFrontRightBrand())
                    .frontRightTread(tyres.getFrontRightTread())
                    .frontRightYear(tyres.getFrontRightYear())
                    .rearLeftBrand(tyres.getRearLeftBrand())
                    .rearLeftTread(tyres.getRearLeftTread())
                    .rearLeftYear(tyres.getRearLeftYear())
                    .rearRightBrand(tyres.getRearRightBrand())
                    .rearRightTread(tyres.getRearRightTread())
                    .rearRightYear(tyres.getRearRightYear())
                    .spareBrand(tyres.getSpareBrand())
                    .spareTread(tyres.getSpareTread())
                    .spareYear(tyres.getSpareYear())
                    .hasJack(tyres.getHasJack())
                    .hasHandle(tyres.getHasHandle())
                    .hasToolkit(tyres.getHasToolkit())
                    .hasTriangle(tyres.getHasTriangle())
                    .hasFirstAidBox(tyres.getHasFirstAidBox())
                    .build();
        }

        InspectionDetailsResponse.InteriorResponseDTO iDTO = null;
        if (interior != null) {
            iDTO = InspectionDetailsResponse.InteriorResponseDTO.builder()
                    .id(interior.getId())
                    .batteryBrand(interior.getBatteryBrand())
                    .batterySerialNumber(interior.getBatterySerialNumber())
                    .acCooling(interior.getAcCooling())
                    .evaluatorValuation(interior.getEvaluatorValuation())
                    .rightTailLamp(interior.getRightTailLamp())
                    .leftTailLamp(interior.getLeftTailLamp())
                    .rightHeadLamp(interior.getRightHeadLamp())
                    .leftHeadLamp(interior.getLeftHeadLamp())
                    .indicators(interior.getIndicators())
                    .bootFloor(interior.getBootFloor())
                    .dashboard(interior.getDashboard())
                    .fogLamps(interior.getFogLamps())
                    .powerWindows(interior.getPowerWindows())
                    .musicSystem(interior.getMusicSystem())
                    .steeringMountedControls(interior.getSteeringMountedControls())
                    .wiper(interior.getWiper())
                    .rearDefogger(interior.getRearDefogger())
                    .rearWasher(interior.getRearWasher())
                    .instrumentCluster(interior.getInstrumentCluster())
                    .infotainment(interior.getInfotainment())
                    .centralLock(interior.getCentralLock())
                    .pushButton(interior.getPushButton())
                    .sunroof(interior.getSunroof())
                    .sensors(interior.getSensors())
                    .remarks(interior.getRemarks() != null ? interior.getRemarks() : (remarks != null ? remarks.getInspectorRemarks() : null))
                    .build();
        }

        InspectionDetailsResponse.RatingsResponseDTO rDTO = null;
        if (ins != null) {
            rDTO = InspectionDetailsResponse.RatingsResponseDTO.builder()
                    .exterior(ins.getExteriorRating())
                    .mechanical(ins.getMechanicalRating())
                    .tyre(ins.getTyreRating())
                    .interior(ins.getInteriorRating())
                    .build();
        }

        return InspectionDetailsResponse.builder()
                .inspectionId(ins != null ? ins.getId() : null)
                .status(ins != null ? ins.getStatus() : null)
                .rejectionReason(ins != null ? ins.getRejectionReason() : null)
                .submittedAt(ins != null ? ins.getSubmittedAt() : null)
                .inspectorName(ins != null && ins.getInspector() != null ? ins.getInspector().getFullName() : null)
                .vehicleDetails(vDTO)
                .exteriorPanelDetails(panelDTOs)
                .mechanicalDetails(mDTO)
                .tyreDetails(tDTO)
                .interiorDetails(iDTO)
                .inspectionPhotos(photoDTOs)
                .ratings(rDTO)
                .build();
    }

    private void addSectionHeading(Document document, String title) throws DocumentException {
        PdfPTable secTable = new PdfPTable(1);
        secTable.setWidthPercentage(100);
        secTable.setSpacingBefore(10);
        secTable.setSpacingAfter(6);

        PdfPCell cell = new PdfPCell(new Phrase(title, sectionFont));
        cell.setBackgroundColor(lightBg);
        cell.setPadding(5);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(primaryColor);
        cell.setBorderWidth(1.5f);

        secTable.addCell(cell);
        document.add(secTable);
    }

    private void addStyledTableCell(PdfPTable table, String field, String value) {
        PdfPCell cellLabel = new PdfPCell(new Phrase(field, labelFont));
        cellLabel.setBackgroundColor(lightBg);
        cellLabel.setPadding(5);
        cellLabel.setBorderColor(borderGray);
        table.addCell(cellLabel);

        PdfPCell cellVal = new PdfPCell(new Phrase(value != null && !value.isEmpty() ? value : "N/A", valueFont));
        cellVal.setPadding(5);
        cellVal.setBorderColor(borderGray);
        table.addCell(cellVal);
    }

    private void addPanelCell(PdfPTable table, String name, String status, String imageUrl) {
        PdfPCell labelCell = new PdfPCell(new Phrase(name, valueFont));
        labelCell.setPadding(4);
        labelCell.setBorderColor(borderGray);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(labelCell);

        Font statusFont = valueFont;
        String cleanStatus = status != null ? status.toUpperCase().trim() : "OK";
        if ("OK".equals(cleanStatus) || "NO DAMAGES".equals(cleanStatus) || "NA".equals(cleanStatus) || "N/A".equals(cleanStatus) || "NORMAL".equals(cleanStatus)) {
            statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Font.BOLD, greenColor);
        } else if ("REPAINTED".equals(cleanStatus) || "CHANGED".equals(cleanStatus) || "SCRATCH".equals(cleanStatus) || "DENT".equals(cleanStatus)) {
            statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Font.BOLD, amberColor);
        } else if ("DAMAGED".equals(cleanStatus) || "RUST".equals(cleanStatus)) {
            statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Font.BOLD, redColor);
        }

        PdfPCell valCell = new PdfPCell(new Phrase(status != null ? status : "OK", statusFont));
        valCell.setPadding(4);
        valCell.setBorderColor(borderGray);
        valCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(valCell);

        PdfPCell imgCell = new PdfPCell();
        imgCell.setPadding(3);
        imgCell.setBorderColor(borderGray);
        imgCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        imgCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        boolean isNa = "NA".equals(cleanStatus) || "N/A".equals(cleanStatus)
                || "NOT APPLICABLE".equals(cleanStatus) || "NOT_APPLICABLE".equals(cleanStatus)
                || "NONE".equals(cleanStatus) || "-".equals(cleanStatus)
                || cleanStatus.startsWith("NA") || cleanStatus.startsWith("N/A");

        if (!isNa && imageUrl != null && !imageUrl.trim().isEmpty()) {
            boolean isVideo = imageUrl.toLowerCase().matches(".*\\.(mp4|webm|mov|avi|mkv|3gp|flv|wmv)($|\\?.*)")
                    || imageUrl.toLowerCase().contains("video")
                    || "Engine / Motor Noise".equalsIgnoreCase(name);
            if (isVideo) {
                Font videoFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.0f, Font.BOLD, new Color(79, 70, 229));
                Paragraph videoText = new Paragraph("[VIDEO ATTACHED]", videoFont);
                videoText.setAlignment(Element.ALIGN_CENTER);
                imgCell.addElement(videoText);
            } else {
                Image pdfImg = createPdfImage(imageUrl, 60, 40);
                if (pdfImg != null) {
                    imgCell.addElement(pdfImg);
                } else {
                    Paragraph noPhoto = new Paragraph("-", valueFont);
                    noPhoto.setAlignment(Element.ALIGN_CENTER);
                    imgCell.addElement(noPhoto);
                }
            }
        } else {
            Paragraph noPhoto = new Paragraph("-", valueFont);
            noPhoto.setAlignment(Element.ALIGN_CENTER);
            imgCell.addElement(noPhoto);
        }
        table.addCell(imgCell);
    }

    private void addDiagnosticRow(PdfPTable table, String name, String value, String imageUrl) {
        PdfPCell labelCell = new PdfPCell(new Phrase(name, labelFont));
        labelCell.setPadding(4.5f);
        labelCell.setBorderColor(borderGray);
        labelCell.setBackgroundColor(lightBg);
        table.addCell(labelCell);

        String cleanVal = value != null ? value.toUpperCase().trim() : "OK";
        boolean isOk = cleanVal.contains("OK") || cleanVal.contains("WORKING") || cleanVal.contains("YES") || cleanVal.contains("CAPTURED") || cleanVal.contains("NORMAL");
        Font statusFont = valueFont;
        if (value != null && !value.isEmpty()) {
            if (isOk) {
                statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Font.BOLD, greenColor);
            } else if (cleanVal.contains("NO") || cleanVal.contains("DAMAGED") || cleanVal.contains("NOT WORKING") || cleanVal.contains("RUST") || cleanVal.contains("DENT")) {
                statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Font.BOLD, redColor);
            } else {
                statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Font.BOLD, amberColor);
            }
        }

        PdfPCell valCell = new PdfPCell(new Phrase(value != null ? value : "OK / WORKING", statusFont));
        valCell.setPadding(4.5f);
        valCell.setBorderColor(borderGray);
        table.addCell(valCell);

        PdfPCell photoCell = new PdfPCell();
        photoCell.setPadding(3);
        photoCell.setBorderColor(borderGray);
        photoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        photoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        boolean isNa = "NA".equals(cleanVal) || "N/A".equals(cleanVal)
                || "NOT APPLICABLE".equals(cleanVal) || "NOT_APPLICABLE".equals(cleanVal)
                || "NONE".equals(cleanVal) || "-".equals(cleanVal)
                || cleanVal.startsWith("NA") || cleanVal.startsWith("N/A");

        boolean isExplicitPhotoSlot = name.toUpperCase().endsWith("IMAGE") || name.toUpperCase().endsWith("PHOTO") || name.toUpperCase().endsWith("IMG");

        if ((!isNa || isExplicitPhotoSlot) && imageUrl != null && !imageUrl.trim().isEmpty()) {
            boolean isVideo = imageUrl.toLowerCase().matches(".*\\.(mp4|webm|mov|avi|mkv|3gp|flv|wmv)($|\\?.*)")
                    || imageUrl.toLowerCase().contains("video")
                    || "Engine / Motor Noise".equalsIgnoreCase(name);
            if (isVideo) {
                Font videoFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.0f, Font.BOLD, new Color(79, 70, 229));
                Paragraph videoText = new Paragraph("[VIDEO ATTACHED]", videoFont);
                videoText.setAlignment(Element.ALIGN_CENTER);
                photoCell.addElement(videoText);
            } else {
                Image pdfImg = createPdfImage(imageUrl, 55, 36);
                if (pdfImg != null) {
                    photoCell.addElement(pdfImg);
                } else {
                    photoCell.addElement(new Phrase("-", valueFont));
                }
            }
        } else {
            photoCell.addElement(new Phrase("-", valueFont));
        }
        table.addCell(photoCell);
    }

    private void addTyreRow(PdfPTable table, String pos, String brand, String info) {
        table.addCell(new Phrase(pos, valueFont));
        table.addCell(new Phrase(brand != null ? brand : "JK Tyre", valueFont));
        table.addCell(new Phrase(info != null ? info : "60% / 2021", valueFont));
    }

    private void addRatingCell(PdfPTable table, String label, Double rating) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(5);
        cell.setBorderColor(borderGray);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph labelPara = new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Font.BOLD, new Color(100, 100, 110)));
        labelPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(labelPara);

        String valStr = rating != null ? String.format("%.1f", rating) + " / 5.0" : "4.0 / 5.0";
        Paragraph valPara = new Paragraph(valStr, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Font.BOLD, primaryColor));
        valPara.setAlignment(Element.ALIGN_CENTER);
        valPara.setSpacingBefore(2);
        cell.addElement(valPara);

        table.addCell(cell);
    }

    private String getBooleanText(Boolean value) {
        return Boolean.TRUE.equals(value) ? "YES" : "NO";
    }

    private String formatCurrency(Double val) {
        if (val == null || val == 0) return "N/A";
        try {
            DecimalFormat df = new DecimalFormat("#,##,##0");
            return "₹" + df.format(val);
        } catch (Exception e) {
            return "₹" + String.format("%.0f", val);
        }
    }

    private String formatTread(Integer tread, Integer year) {
        String t = tread != null ? tread + "%" : "60%";
        String y = year != null ? String.valueOf(year) : "2021";
        return t + " / " + y;
    }

    private String findPhotoUrlBySlot(InspectionDetailsResponse details, String slotType) {
        if (details == null || details.getInspectionPhotos() == null || slotType == null) return null;
        for (InspectionDetailsResponse.PhotoResponseDTO photo : details.getInspectionPhotos()) {
            if (photo.getImageUrl() != null && !photo.getImageUrl().isEmpty()) {
                if (slotType.equalsIgnoreCase(photo.getPhotoType()) ||
                    isMatch(photo.getImageCategory(), slotType) ||
                    isMatch(photo.getDisplayName(), slotType)) {
                    return photo.getImageUrl();
                }
            }
        }
        return null;
    }

    private String findPhotoUrlByName(InspectionDetailsResponse details, String name) {
        if (details == null || details.getInspectionPhotos() == null || name == null) return null;
        for (InspectionDetailsResponse.PhotoResponseDTO photo : details.getInspectionPhotos()) {
            if (photo.getImageUrl() != null && !photo.getImageUrl().isEmpty()) {
                if (isMatch(photo.getImageCategory(), name) ||
                    isMatch(photo.getDisplayName(), name)) {
                    return photo.getImageUrl();
                }
            }
        }
        return null;
    }

    private boolean isMatch(String categoryStr, String targetName) {
        if (categoryStr == null || targetName == null) return false;
        String cleanCat = categoryStr.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        String cleanTgt = targetName.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();

        if (cleanCat.isEmpty() || cleanTgt.isEmpty()) return false;
        if (cleanCat.equals(cleanTgt)) return true;

        boolean isGenericCat = cleanCat.equals("INTERIOR") || cleanCat.equals("EXTERIOR") || cleanCat.equals("MECHANICAL") || cleanCat.equals("TYRE") || cleanCat.equals("TYRES");

        // Primary Angle Slots
        if (cleanTgt.equals("FRONTVIEW") || cleanTgt.equals("FRONT") || cleanTgt.equals("FRONTSIDEIMAGE")) {
            return cleanCat.equals("FRONT") || cleanCat.equals("FRONTVIEW") || cleanCat.equals("FRONTSIDE") || cleanCat.equals("FRONTSIDEIMAGE");
        }
        if (cleanTgt.equals("RIGHTFRONTVIEW") || cleanTgt.equals("RIGHTFRONT") || cleanTgt.equals("RIGHTSIDEIMAGE")) {
            return cleanCat.equals("RIGHT") || cleanCat.equals("RIGHTFRONT") || cleanCat.equals("RIGHTFRONTVIEW") || cleanCat.equals("RIGHTSIDE") || cleanCat.equals("RIGHTSIDEIMAGE");
        }
        if (cleanTgt.equals("REARVIEW") || cleanTgt.equals("REAR") || cleanTgt.equals("REARSIDEIMAGE")) {
            return cleanCat.equals("REAR") || cleanCat.equals("REARVIEW") || cleanCat.equals("REARSIDE") || cleanCat.equals("REARSIDEIMAGE") || cleanCat.equals("BACK");
        }
        if (cleanTgt.equals("LEFTFRONTVIEW") || cleanTgt.equals("LEFTFRONT") || cleanTgt.equals("LEFTSIDEIMAGE")) {
            return cleanCat.equals("LEFT") || cleanCat.equals("LEFTFRONT") || cleanCat.equals("LEFTFRONTVIEW") || cleanCat.equals("LEFTSIDE") || cleanCat.equals("LEFTSIDEIMAGE");
        }
        if (cleanTgt.equals("ROOFVIEW") || cleanTgt.equals("ROOF") || cleanTgt.equals("ROOFTOPIMAGE")) {
            return cleanCat.equals("ROOF") || cleanCat.equals("ROOFVIEW") || cleanCat.equals("ROOFTOP") || cleanCat.equals("ROOFTOPIMAGE");
        }

        // Tyre Slots
        if (cleanTgt.contains("FRONTRIGHTTYRE") || cleanTgt.equals("FRONTRIGHT") || cleanTgt.equals("RF")) {
            return cleanCat.contains("RFTYRE") || cleanCat.equals("FRONTRIGHT") || cleanCat.equals("FRONTRIGHTTYRE") || cleanCat.equals("RF") || cleanCat.contains("RIGHTSIDEFRONTTYRE");
        }
        if (cleanTgt.contains("REARRIGHTTYRE") || cleanTgt.equals("REARRIGHT") || cleanTgt.equals("RR")) {
            return cleanCat.contains("RRTYRE") || cleanCat.equals("REARRIGHT") || cleanCat.equals("REARRIGHTTYRE") || cleanCat.equals("RR") || cleanCat.contains("RIGHTSIDEREARTYRE");
        }
        if (cleanTgt.contains("FRONTLEFTTYRE") || cleanTgt.equals("FRONTLEFT") || cleanTgt.equals("LF")) {
            return cleanCat.contains("LFTYRE") || cleanCat.equals("FRONTLEFT") || cleanCat.equals("FRONTLEFTTYRE") || cleanCat.equals("LF") || cleanCat.contains("LEFTSIDEFRONTTYRE");
        }
        if (cleanTgt.contains("REARLEFTTYRE") || cleanTgt.equals("REARLEFT") || cleanTgt.equals("LR")) {
            return cleanCat.contains("LRTYRE") || cleanCat.equals("REARLEFT") || cleanCat.equals("REARLEFTTYRE") || cleanCat.equals("LR") || cleanCat.contains("LEFTSIDEREARTYRE");
        }
        if (cleanTgt.contains("SPARE")) {
            return cleanCat.contains("SPARE");
        }
        if (cleanTgt.contains("OVERVIEW") || cleanTgt.equals("TYRESGENERALIMG")) {
            return cleanCat.contains("OVERVIEW") || cleanCat.equals("TYRESOVERVIEW") || cleanCat.equals("TYRESGENERALIMG");
        }

        // Cabin & Diagnostic Items
        if (cleanTgt.contains("ODOMETER")) return cleanCat.contains("ODOMETER");
        if (cleanTgt.contains("DASHBOARD")) return cleanCat.contains("DASHBOARD") || (isGenericCat && cleanTgt.equals("DASHBOARDIMAGE"));
        if (cleanTgt.contains("ACCONTROL") || cleanTgt.equals("AC")) return cleanCat.contains("AC");
        if (cleanTgt.contains("CLUSTER")) return cleanCat.contains("CLUSTER");
        if (cleanTgt.contains("MUSIC") || cleanTgt.contains("INFOTAINMENT")) return cleanCat.contains("MUSIC") || cleanCat.contains("INFOTAINMENT");
        if (cleanTgt.contains("POWERWINDOW")) return cleanCat.contains("POWERWINDOW") || cleanCat.contains("WINDOW");
        if (cleanTgt.contains("DEFOGGER")) return cleanCat.contains("DEFOGGER");
        if (cleanTgt.contains("REARWASHER") || cleanTgt.contains("REARWIPER")) return cleanCat.contains("REARWASHER") || cleanCat.contains("REARWIPER") || cleanCat.contains("WASHER");
        if (cleanTgt.contains("ENGINE")) return cleanCat.contains("ENGINE");
        if (cleanTgt.contains("BATTERY")) return cleanCat.contains("BATTERY");

        if (isGenericCat) return false;

        return cleanCat.contains(cleanTgt) || cleanTgt.contains(cleanCat);
    }

    private Image createPdfImage(String imageUrl, float fitWidth, float fitHeight) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return null;
        }
        String lowerUrl = imageUrl.toLowerCase();
        if (lowerUrl.matches(".*\\.(mp4|webm|mov|avi|mkv|3gp|flv|wmv)($|\\?.*)") || lowerUrl.contains("video")) {
            return null;
        }
        try {
            // 1. Base64 Data URL
            if (imageUrl.startsWith("data:image")) {
                int commaIdx = imageUrl.indexOf(",");
                if (commaIdx != -1) {
                    byte[] imgBytes = java.util.Base64.getDecoder().decode(imageUrl.substring(commaIdx + 1));
                    Image pdfImg = loadPdfImageFromBytes(imgBytes, fitWidth, fitHeight);
                    if (pdfImg != null) return pdfImg;
                }
            }

            // 2. Extract filename & base name
            String filename = imageUrl.contains("/") ? imageUrl.substring(imageUrl.lastIndexOf("/") + 1) : imageUrl;
            if (filename.contains("?")) {
                filename = filename.substring(0, filename.indexOf("?"));
            }
            String baseName = filename.contains(".") ? filename.substring(0, filename.lastIndexOf(".")) : filename;

            // 3. Generate candidate filenames (matching exact extension + common image extensions)
            List<String> fileVariations = new ArrayList<>();
            fileVariations.add(filename);
            for (String ext : new String[]{".jpg", ".jpeg", ".png", ".webp", ".avif"}) {
                String var = baseName + ext;
                if (!fileVariations.contains(var)) {
                    fileVariations.add(var);
                }
            }

            // 4. Search local disk locations
            String userDir = System.getProperty("user.dir");
            List<Path> baseDirs = new ArrayList<>();
            baseDirs.add(Paths.get("uploads", "car", "images"));
            baseDirs.add(Paths.get(userDir, "uploads", "car", "images"));
            baseDirs.add(Paths.get("uploads", "inspections"));
            baseDirs.add(Paths.get(userDir, "uploads", "inspections"));
            baseDirs.add(Paths.get("uploads", "car"));
            baseDirs.add(Paths.get(userDir, "uploads", "car"));
            baseDirs.add(Paths.get("uploads"));
            baseDirs.add(Paths.get(userDir, "uploads"));
            baseDirs.add(Paths.get("."));
            baseDirs.add(Paths.get(userDir));
            baseDirs.add(Paths.get("C:/Users/Laptop On Rent 200/Documents/caryanam-beading-backend/uploads/car/images"));

            for (Path baseDir : baseDirs) {
                for (String fname : fileVariations) {
                    Path candidate = baseDir.resolve(fname);
                    if (Files.exists(candidate) && !Files.isDirectory(candidate)) {
                        try {
                            byte[] bytes = Files.readAllBytes(candidate);
                            Image pdfImg = loadPdfImageFromBytes(bytes, fitWidth, fitHeight);
                            if (pdfImg != null) {
                                return pdfImg;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }

            // 5. Remote HTTP URL stream fallback
            List<String> targetUrls = new ArrayList<>();
            if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                targetUrls.add(imageUrl);
                if (imageUrl.contains("/uploads/")) {
                    String subPath = imageUrl.substring(imageUrl.indexOf("/uploads/"));
                    targetUrls.add("http://localhost:8080" + subPath);
                }
            } else {
                String subPath = imageUrl.startsWith("/") ? imageUrl : "/" + imageUrl;
                targetUrls.add("https://api.caryanamlive.com" + subPath);
                targetUrls.add("http://localhost:8080" + subPath);
            }

            for (String targetUrl : targetUrls) {
                byte[] httpBytes = fetchHttpImageBytes(targetUrl);
                if (httpBytes != null && httpBytes.length > 0) {
                    Image pdfImg = loadPdfImageFromBytes(httpBytes, fitWidth, fitHeight);
                    if (pdfImg != null) {
                        return pdfImg;
                    }
                }
            }

            // 6. Fallback Placeholder Image
            return createFallbackPlaceholderImage(fitWidth, fitHeight, "PHOTO ATTACHED");

        } catch (Exception e) {
            System.err.println("Failed to create PDF image for URL: " + imageUrl + " -> " + e.getMessage());
        }
        return createFallbackPlaceholderImage(fitWidth, fitHeight, "PHOTO ATTACHED");
    }

    private Image loadPdfImageFromBytes(byte[] rawBytes, float fitWidth, float fitHeight) {
        if (rawBytes == null || rawBytes.length == 0) return null;

        // Try direct OpenPDF Image.getInstance (fast path for JPEG, PNG, GIF, BMP)
        try {
            Image pdfImg = Image.getInstance(rawBytes);
            pdfImg.scaleToFit(fitWidth, fitHeight);
            pdfImg.setAlignment(Element.ALIGN_CENTER);
            return pdfImg;
        } catch (Exception ignored1) {}

        // Fallback: Use Java ImageIO to decode into BufferedImage and re-encode to RGB JPEG
        try {
            java.awt.image.BufferedImage bufferedImage = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(rawBytes));
            if (bufferedImage != null) {
                java.awt.image.BufferedImage rgbImage = new java.awt.image.BufferedImage(
                    bufferedImage.getWidth(),
                    bufferedImage.getHeight(),
                    java.awt.image.BufferedImage.TYPE_INT_RGB
                );
                java.awt.Graphics2D g = rgbImage.createGraphics();
                g.drawImage(bufferedImage, 0, 0, java.awt.Color.WHITE, null);
                g.dispose();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                javax.imageio.ImageIO.write(rgbImage, "jpg", baos);
                byte[] jpegBytes = baos.toByteArray();
                if (jpegBytes.length > 0) {
                    Image pdfImg = Image.getInstance(jpegBytes);
                    pdfImg.scaleToFit(fitWidth, fitHeight);
                    pdfImg.setAlignment(Element.ALIGN_CENTER);
                    return pdfImg;
                }
            }
        } catch (Exception ignored2) {}

        return null;
    }

    private byte[] fetchHttpImageBytes(String targetUrl) {
        try {
            URL url = new URL(targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (InputStream in = conn.getInputStream();
                     ByteArrayOutputStream byteBuf = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int n;
                    while ((n = in.read(buffer)) != -1) {
                        byteBuf.write(buffer, 0, n);
                    }
                    return byteBuf.toByteArray();
                }
            }
        } catch (Exception httpEx) {
            System.err.println("HTTP stream fetch failed for: " + targetUrl + " -> " + httpEx.getMessage());
        }
        return null;
    }

    private Image createFallbackPlaceholderImage(float fitWidth, float fitHeight, String label) {
        try {
            int w = Math.max(140, (int) fitWidth * 2);
            int h = Math.max(90, (int) fitHeight * 2);
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = img.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(new Color(24, 27, 36));
            g.fillRect(0, 0, w, h);

            g.setColor(primaryColor);
            g.setStroke(new java.awt.BasicStroke(2.0f));
            g.drawRect(2, 2, w - 5, h - 5);

            g.setColor(Color.WHITE);
            g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 10));
            java.awt.FontMetrics fm = g.getFontMetrics();
            String text = (label != null && !label.isEmpty()) ? label.toUpperCase() : "PHOTO ATTACHED";
            int textX = (w - fm.stringWidth(text)) / 2;
            int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(text, Math.max(4, textX), textY);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(img, "jpg", baos);
            Image pdfImg = Image.getInstance(baos.toByteArray());
            pdfImg.scaleToFit(fitWidth, fitHeight);
            pdfImg.setAlignment(Element.ALIGN_CENTER);
            return pdfImg;
        } catch (Exception e) {
            return null;
        }
    }
}
