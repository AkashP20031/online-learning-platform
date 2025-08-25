package com.notification.services;

import com.notification.dtos.CourseDto;
import com.notification.dtos.OfferDto;
import com.notification.dtos.PaymentResponseDto;
import com.notification.dtos.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.Set;

@Service
public class NotificationService {

        @Autowired
        private RestTemplate restTemplate;

        @Autowired
        private JavaMailSender mailSender;

        @Autowired
        private SimpMessagingTemplate messagingTemplate;

        public void notifyUsersAboutOffers() {
            try {
                ResponseEntity<UserDto[]> userResponse = restTemplate.getForEntity(
                        "http://USERSERVICE/api/users/", UserDto[].class);
                UserDto[] users = userResponse.getBody();

                if (users == null || users.length == 0) {
                    System.out.println("No users found to notify.");
                    return;
                }

                ResponseEntity<OfferDto[]> offerResponse = restTemplate.getForEntity(
                        "http://OFFERSERVICE/api/offers/active", OfferDto[].class);
                OfferDto[] offers = offerResponse.getBody();

                if (offers == null || offers.length == 0) {
                    System.out.println("No active offers to notify about.");
                    return;
                }
                StringBuilder offerMessage = new StringBuilder("\uD83D\uDD25 Today's Active Course Offers:\n\n");
                for (OfferDto offer : offers) {
                    offerMessage.append("\uD83C\uDF81 Coupon: ").append(offer.getCode())
                            .append(" - ").append("get offer on course upto ")
                            .append(" (").append(offer.getDiscountPercentage()).append("% OFF)\n");
                }

                Set<String> uniqueEmails = new HashSet<>();
                for (UserDto user : users) {
                    if (user.getEmail() != null && !user.getEmail().isBlank() && uniqueEmails.add(user.getEmail())) {
                        sendEmail(user.getEmail(), "🔥 Don't miss these course discounts!", offerMessage.toString());
                        messagingTemplate.convertAndSend(
                                "/topic/notifications/" + user.getUsername(),
                                "🔥 New Offers Available! Check your email for details."
                        );
                    }
                }

            } catch (Exception e) {
                System.out.println("Error during notification process: " + e.getMessage());
            }
        }

    @KafkaListener(topics = "notification-topic", groupId = "notification-group")
    public void consume(PaymentResponseDto paymentResponse) {
        try {
            System.out.println("📩 Received Payment Event: " + paymentResponse);

            ResponseEntity<UserDto> userResponse = restTemplate.getForEntity(
                    "http://USERSERVICE/api/users/" + paymentResponse.getUserId(), UserDto.class);
            UserDto user = userResponse.getBody();

            ResponseEntity<CourseDto> courseResponse = restTemplate.getForEntity(
                    "http://COURSESERVICE/api/course/" + paymentResponse.getCourseId(), CourseDto.class);
            CourseDto course = courseResponse.getBody();

            String subject = "✅Payment Confirmation - Order " + paymentResponse.getPaymentId();
            if (user != null &&  course != null) {
                String body = "Hello " + user.getUsername() + ",\n\n" +
                        "We received your payment of ₹" + paymentResponse.getAmount() +
                        " for Course: " + course.getTitle() + ".\n\n" +
                        "Payment via: " + paymentResponse.getPaymentGateway() + ".\n\n" +
                        "🎉 Thank you for your purchase! Happy Learning 🚀";

                sendEmail(user.getEmail(), subject, body);

                messagingTemplate.convertAndSend(
                        "/topic/notifications/" + user.getUsername(),
                        "✅ Payment of ₹" + paymentResponse.getAmount() +
                                " for course " + course.getTitle() + " was successful!"
                );
            }
        } catch (Exception e) {
            System.out.println("Error during notification process: " + e.getMessage());
        }
    }


    private void sendEmail(String to, String subject, String body) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        }
    }

