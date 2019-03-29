package root.repositories

import org.springframework.data.repository.CrudRepository
import root.data.entity.tasks.RegisterOnExchange

interface RegisterOnExchangeRepository : CrudRepository<RegisterOnExchange, Long> {

}
