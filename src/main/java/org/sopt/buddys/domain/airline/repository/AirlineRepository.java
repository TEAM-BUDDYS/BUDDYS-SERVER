package org.sopt.buddys.domain.airline.repository;

import org.sopt.buddys.domain.airline.entity.Airline;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AirlineRepository extends JpaRepository<Airline, Long> {

  @Query("""
      select a
      from Airline a
      where lower(a.name) like lower(concat('%', :keyword, '%')) escape '\\'
         or lower(a.code) like lower(concat('%', :keyword, '%')) escape '\\'
      order by a.name asc
      """)
  Slice<Airline> search(@Param("keyword") String keyword, Pageable pageable);
}
