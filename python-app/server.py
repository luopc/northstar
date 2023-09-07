import os
import time
import random
import numpy as np
import torch as th
import gymnasium as gym
from flask import Flask, request, jsonify
from buffer import TradingRolloutBuffer
from stable_baselines3 import PPO
from stable_baselines3.common.utils import obs_as_tensor
from env import TradingEnv

app = Flask(__name__)

AGENTS = {
    "ppo": PPO
}

# env = TradingEnv()
# model = PPO("MlpPolicy", env, verbose=1)
# obs = env.reset()

last_action = [0]


class Server:
    def __init__(self) -> None:
        self.agent_name = None
        self.is_train = None
        self.models = {}
        self.observation_space = gym.spaces.Box(low=0, high=np.inf, shape=(4,), dtype=np.float32)
        self.action_space = gym.spaces.Discrete(3)
        self.rollout_buffer = TradingRolloutBuffer(
            buffer_size=100000,
            observation_space=self.observation_space,
            action_space=self.action_space,
        )
        
    def get_model_id(self, indicator_symbol, model_version):
        return f"{indicator_symbol}-{self.agent_name}-{model_version}"
    
    def load_model(self, indicator_symbol, model_version):
        model = AGENTS[self.agent_name]("MlpPolicy")
        model_id = self.get_model_id(indicator_symbol, model_version)
        self.models[indicator_symbol] = model
        
    def load_model_from_file(self, indicator_symbol, model_version):
        model_id = self.get_model_id(indicator_symbol, model_version)
        model_path = f"models/{model_id}"
        if os.exists(model_path):
            model = AGENTS[self.agent_name]("MlpPolicy")
            model.load(model_path)
            self.models[indicator_symbol] = model
            return True
        else:
            return False
    
    @app.route("/init-info", methods=["POST"])
    def init_info(self):
        # load data from request
        data = request.get_json()
        print(f"init_info: {data}")
        self.agent_name = data["agent_name"].lower()
        self.is_train = data["is_train"]
        indicator_symbol = data["indicator_symbol"]
        model_version = data["model_version"]
        
        # if train, load model from scratch; else, load model from file
        if self.is_train:
            self.load_model(indicator_symbol, model_version)
            return jsonify({"success": True})
        else:
            if self.load_model_from_file(indicator_symbol, model_version):
                return jsonify({"success": True})
            else:
                return jsonify({"success": False})


    @app.route("/get-action", methods=["POST"])
    def get_action(self):
        global last_action
        start_time = time.time()
        
        # load data from request
        data = request.get_json()
        unified_symbol = data["unified_symbol"]
        open_price = data["open_price"]
        high_price = data["high_price"]
        low_price = data["low_price"]
        close_price = data["close_price"]
        last_reward = data["last_reward"]
        
        model = self.models[unified_symbol]
        state = [open_price, high_price, low_price, close_price]
       
        with th.no_grad():
            # Convert to pytorch tensor or to TensorDict
            obs_tensor = obs_as_tensor(self._last_obs, self.device)
            actions, values, log_probs = self.model.policy(obs_tensor)
        actions = actions.cpu().numpy()
        
        self.rollout_buffer.add(
            obs_tensor,
            actions,
            last_reward,
            self._last_episode_starts,
            values,
        )
        
        
        # if self.is_train:
        #     self.save_to_buffer()
        
    
        return jsonify({"action": int(np.argmax(actions))})
    
if __name__ == "__main__":
    app.run(port=5001)