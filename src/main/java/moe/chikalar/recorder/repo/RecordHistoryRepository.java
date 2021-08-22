package moe.chikalar.recorder.repo;

import moe.chikalar.recorder.entity.RecordHistory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;

@Repository
public interface RecordHistoryRepository extends CrudRepository<RecordHistory, Long>, PagingAndSortingRepository<RecordHistory, Long> {
    @Transactional
    void deleteByRecordRoomId(Long id);

    List<RecordHistory> findByStatusInAndUploadStatusAndStartTimeBetween(List<String> status,
                                                                         String uploadStatus,
                                                                         Date from,
                                                                         Date to);

    List<RecordHistory> findByStartTimeBetween(Date start, Date end);
}
