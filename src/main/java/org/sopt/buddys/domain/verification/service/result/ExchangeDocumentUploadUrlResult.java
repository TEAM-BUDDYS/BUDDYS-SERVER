package org.sopt.buddys.domain.verification.service.result;

import java.util.Map;

public record ExchangeDocumentUploadUrlResult(
    String uploadUrl,
    Map<String, String> fields,
    String documentKey
) {
  public ExchangeDocumentUploadUrlResult {
    fields = Map.copyOf(fields);
  }
}
