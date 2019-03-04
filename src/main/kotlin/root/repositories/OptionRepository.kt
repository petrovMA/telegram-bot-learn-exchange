package root.repositories

import org.springframework.data.repository.CrudRepository
import root.data.entity.Option

interface OptionRepository : CrudRepository<Option, Long>
