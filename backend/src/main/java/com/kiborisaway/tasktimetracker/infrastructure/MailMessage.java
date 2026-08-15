package com.kiborisaway.tasktimetracker.infrastructure;

public record MailMessage(String to, String subject, String htmlBody, String textBody) {
}