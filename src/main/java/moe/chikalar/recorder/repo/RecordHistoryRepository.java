package moe.chikalar.recorder.repo;

import moe.chikalar.recorder.entity.RecordHistory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecordHistoryRepository extends CrudRepository<RecordHistory, Long>, PagingAndSortingRepository<RecordHistory, Long> {
    @Transactional
    void deleteByRecordRoomId(Long id);

    List<RecordHistory> findByStatusInAndUploadStatusAndUploadRetryCountLessThanAndUpdateTimeBetweenOrderByStartTimeAsc
            (List<String> status,
             String uploadStatus,
             Integer retryCount,
             Date from,
             Date to);

    List<RecordHistory> findByStartTimeBetween(Date start, Date end);


    Optional<RecordHistory> findTop1ByRecordRoomIdOrderByStartTimeDesc(Long roomId);

    List<RecordHistory>  findByStatusInAndUploadStatusAndUploadRetryCountLessThanAndRealStartTimeOrderByStartTimeAsc(List<String> asList, String s, int toIntExact, Long realStartTime);
}
