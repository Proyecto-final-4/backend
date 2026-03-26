package com.backend.backend.domain.savingsgoal;

import com.backend.backend.domain.user.User;
import com.backend.backend.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "savings_goals")
public class SavingsGoal extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(length = 500, nullable = true)
  private String description;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal targetAmount;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal currentAmount = BigDecimal.ZERO;

  @Column(nullable = true)
  private LocalDate targetDate;

  @Column(nullable = false)
  private boolean isCompleted = false;

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public BigDecimal getTargetAmount() {
    return targetAmount;
  }

  public void setTargetAmount(BigDecimal targetAmount) {
    this.targetAmount = targetAmount;
  }

  public BigDecimal getCurrentAmount() {
    return currentAmount;
  }

  public void setCurrentAmount(BigDecimal currentAmount) {
    this.currentAmount = currentAmount;
  }

  public LocalDate getTargetDate() {
    return targetDate;
  }

  public void setTargetDate(LocalDate targetDate) {
    this.targetDate = targetDate;
  }

  public boolean isCompleted() {
    return isCompleted;
  }

  public void setCompleted(boolean isCompleted) {
    this.isCompleted = isCompleted;
  }
}
