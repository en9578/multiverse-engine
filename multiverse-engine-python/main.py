"""
Multiverse Engine - Python AI Layer
FastAPI service for LangGraph-based multiverse simulation.
"""
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import Optional
import uuid

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s - %(message)s")
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Multiverse Engine Python service starting...")
    # TODO: init LangGraph, PostgresSaver, Redis client
    yield
    logger.info("Multiverse Engine Python service shutting down...")


app = FastAPI(
    title="Multiverse Engine",
    description="AI-powered cross-border business multiverse simulation engine",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173", "http://127.0.0.1:5173"],
    allow_methods=["*"],
    allow_headers=["*"],
)


# --- Models ---

class TaskRequest(BaseModel):
    task_id: int
    request_id: str
    product_name: str
    target_market: str
    strategy_desc: str = ""
    trace_id: str = ""


class TaskState(BaseModel):
    task_id: int
    stage: str = "CREATED"
    last_completed: str = ""
    overall_progress: int = 0
    universes: list = []
    errors: list = []


class ResumeRequest(BaseModel):
    trace_id: str


class EngineError(BaseModel):
    error: str
    message: str
    task_id: Optional[int] = None
    trace_id: str = ""


# --- In-memory state store (skeleton, replace with LangGraph + Postgres) ---

_task_store: dict[int, TaskState] = {}


# --- Endpoints (Java <-> Python contract, ch 18) ---

@app.get("/health")
async def health():
    return {"status": "healthy"}


@app.post("/engine/tasks")
async def create_task(req: TaskRequest):
    """Trigger multiverse workflow (ch 18.1)."""
    if req.task_id in _task_store:
        current = _task_store[req.task_id]
        raise HTTPException(
            status_code=409,
            detail=EngineError(
                error="TASK_EXISTS",
                message="Task already exists",
                task_id=req.task_id,
                trace_id=req.trace_id,
            ).model_dump(),
        )
    state = TaskState(task_id=req.task_id, stage="COLLECTING", overall_progress=20)
    _task_store[req.task_id] = state
    logger.info("Task created task_id=%s request_id=%s", req.task_id, req.request_id)
    # TODO: trigger LangGraph workflow
    return {"task_id": req.task_id, "stage": "COLLECTING", "accepted": True}


@app.get("/engine/tasks/{task_id}/state")
async def get_task_state(task_id: int):
    """Query task state (ch 18.2)."""
    if task_id not in _task_store:
        raise HTTPException(
            status_code=404,
            detail=EngineError(
                error="TASK_NOT_FOUND",
                message="Task not found",
                task_id=task_id,
            ).model_dump(),
        )
    state = _task_store[task_id]
    return state.model_dump()


@app.post("/engine/tasks/{task_id}/resume")
async def resume_task(task_id: int, req: ResumeRequest):
    """Resume from checkpoint (ch 18.3)."""
    if task_id not in _task_store:
        raise HTTPException(
            status_code=404,
            detail=EngineError(
                error="TASK_NOT_FOUND",
                message="Task not found",
                task_id=task_id,
                trace_id=req.trace_id,
            ).model_dump(),
        )
    state = _task_store[task_id]
    resumed_from = state.last_completed or "CREATED"
    logger.info("Task resumed task_id=%s from=%s", task_id, resumed_from)
    # TODO: trigger LangGraph resume from checkpoint
    return {"task_id": task_id, "stage": state.stage, "resumed_from": resumed_from, "accepted": True}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)