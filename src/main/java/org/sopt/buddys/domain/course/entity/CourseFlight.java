package org.sopt.buddys.domain.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "course_flight")
public class CourseFlight {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @Column(nullable = false, length = 100)
  private String airline;

  @Column(name = "flight_number", length = 20)
  private String flightNumber;

  @Column(name = "departure_airport", nullable = false, length = 100)
  private String departureAirport;

  @Column(name = "departure_at", nullable = false)
  private LocalDateTime departureAt;

  @Column(name = "arrival_airport", nullable = false, length = 100)
  private String arrivalAirport;

  @Column(name = "arrival_at", nullable = false)
  private LocalDateTime arrivalAt;

  @Column(name = "order_no", nullable = false)
  private Short orderNo = 0;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public CourseFlight(
      Course course,
      String airline,
      String flightNumber,
      String departureAirport,
      LocalDateTime departureAt,
      String arrivalAirport,
      LocalDateTime arrivalAt,
      Short orderNo
  ) {
    this.course = course;
    this.airline = airline;
    this.flightNumber = flightNumber;
    this.departureAirport = departureAirport;
    this.departureAt = departureAt;
    this.arrivalAirport = arrivalAirport;
    this.arrivalAt = arrivalAt;
    this.orderNo = orderNo;
  }
}
