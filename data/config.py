# data/config.py
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    APP_NAME: str = "ododok-backend"
    SECRET_KEY: str
    DATABASE_URL: str  # 예: mysql+aiomysql://user:pass@localhost:3306/ododok?charset=utf8mb4

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

settings = Settings()