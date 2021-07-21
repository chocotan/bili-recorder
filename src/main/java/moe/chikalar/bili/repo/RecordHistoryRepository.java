package moe.chikalar.bili.repo;

import moe.chikalar.bili.entity.RecordHistory;
import moe.chikalar.bili.entity.RecordRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecordHistoryRepository extends CrudRepository<RecordHistory, Long>, PagingAndSortingRepository<RecordHistory, Long> {
}
