package com.youhu.caregiver.receiver.service;

import com.youhu.caregiver.common.exception.BusinessException;
import com.youhu.caregiver.common.util.IdCardUtils;
import com.youhu.caregiver.order.repository.OrderRepository;
import com.youhu.caregiver.receiver.domain.Receiver;
import com.youhu.caregiver.receiver.dto.ReceiverRequest;
import com.youhu.caregiver.receiver.dto.ReceiverView;
import com.youhu.caregiver.receiver.repository.ReceiverRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 被照顾人档案管理（每个用户最多 10 人） */
@Service
@RequiredArgsConstructor
public class ReceiverService {

    private static final int MAX_RECEIVERS = 10;

    private final ReceiverRepository receiverRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public List<ReceiverView> list(Long userId) {
        return receiverRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional
    public ReceiverView create(Long userId, ReceiverRequest request) {
        IdCardUtils.validate(request.idCard());
        if (receiverRepository.countByUserId(userId) >= MAX_RECEIVERS) {
            throw new BusinessException(400, "最多添加 " + MAX_RECEIVERS + " 位被照顾人");
        }
        if (receiverRepository.existsByUserIdAndIdCard(userId, request.idCard())) {
            throw BusinessException.conflict("该被照顾人已存在");
        }
        Receiver receiver = new Receiver();
        receiver.setUserId(userId);
        apply(receiver, request);
        return toView(receiverRepository.save(receiver));
    }

    @Transactional
    public ReceiverView update(Long userId, Long id, ReceiverRequest request) {
        IdCardUtils.validate(request.idCard());
        Receiver receiver = owned(userId, id);
        if (!receiver.getIdCard().equals(request.idCard())
                && receiverRepository.existsByUserIdAndIdCard(userId, request.idCard())) {
            throw BusinessException.conflict("该被照顾人已存在");
        }
        apply(receiver, request);
        return toView(receiverRepository.save(receiver));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Receiver receiver = owned(userId, id);
        if (orderRepository.existsByReceiverId(receiver.getId())) {
            throw BusinessException.conflict("该被照顾人已有服务订单，不能删除");
        }
        receiverRepository.delete(receiver);
    }

    private Receiver owned(Long userId, Long id) {
        return receiverRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> BusinessException.notFound("被照顾人不存在"));
    }

    private void apply(Receiver receiver, ReceiverRequest request) {
        receiver.setName(request.name());
        receiver.setIdCard(request.idCard());
        receiver.setPhone(blankToNull(request.phone()));
        receiver.setRelation(request.relation());
        receiver.setHealth(request.health());
        receiver.setSpecial(request.special());
        receiver.setGender(IdCardUtils.gender(request.idCard()));
        receiver.setBirth(IdCardUtils.birthDate(request.idCard()));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private ReceiverView toView(Receiver receiver) {
        return new ReceiverView(
                receiver.getId(),
                receiver.getName(),
                receiver.getIdCard(),
                receiver.getPhone(),
                receiver.getRelation(),
                receiver.getHealth(),
                receiver.getSpecial(),
                receiver.getGender() == null ? null : receiver.getGender().name(),
                receiver.getBirth());
    }
}
