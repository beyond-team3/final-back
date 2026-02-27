package com.monsoon.seedflowplus.domain.deal.core.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TempUserResolver {

    private final UserRepository userRepository;

    public TempUser resolve(UserDetails userDetails) {
        if (userDetails == null) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        User user = userRepository.findByLoginId(userDetails.getUsername())
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));

        return new TempUser(
                user.getId(),
                user.getLoginId(),
                user.getRole(),
                user.getEmployee() != null ? user.getEmployee().getId() : null,
                user.getClient() != null ? user.getClient().getId() : null
        );
    }
}
