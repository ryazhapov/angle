package io.ryazhapov.domain.billing

import io.ryazhapov.domain.{UserId, WithdrawalId}

case class Withdrawal(
  id: WithdrawalId,
  teacherId: UserId,
  amount: Int
)
