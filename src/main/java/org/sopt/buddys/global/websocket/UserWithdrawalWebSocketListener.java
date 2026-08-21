package org.sopt.buddys.global.websocket;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.user.event.UserWithdrawnEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserWithdrawalWebSocketListener {

  private final ActiveWebSocketSessionRegistry sessionRegistry;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void disconnectWithdrawnUser(UserWithdrawnEvent event) {
    sessionRegistry.disconnectUser(event.userId());
  }
}
