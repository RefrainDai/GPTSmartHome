import asyncio
import uuid

from .commands import CommandProcessor
from .events import EventHub
from .schemas import Intent, ScheduleRequest


class SchedulerService:
    def __init__(self, hub: EventHub) -> None:
        self.hub = hub
        self.processor: CommandProcessor | None = None
        self._tasks: dict[str, asyncio.Task] = {}

    def bind_processor(self, processor: CommandProcessor) -> None:
        self.processor = processor

    async def schedule(self, request: ScheduleRequest) -> str:
        if not self.processor:
            raise RuntimeError("Scheduler processor not bound")
        job_id = str(uuid.uuid4())
        task = asyncio.create_task(self._run(job_id, request))
        self._tasks[job_id] = task
        await self.hub.log("info", f"已创建定时任务 {job_id}，{request.delay_seconds:.1f} 秒后执行", "scheduler")
        return job_id

    async def _run(self, job_id: str, request: ScheduleRequest) -> None:
        try:
            await asyncio.sleep(request.delay_seconds)
            intent = Intent(device_id=request.device_id, action=request.action, params=request.params, reply="定时任务已执行。")
            await self.processor.execute_intent(intent, source="scheduler")
        finally:
            self._tasks.pop(job_id, None)
