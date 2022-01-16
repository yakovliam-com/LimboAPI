/*
 * Copyright (C) 2021 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.limboapi.server;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.title.GenericTitlePacket;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.material.Item;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limboapi.api.player.GameMode;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limboapi.api.protocol.map.MapPalette;
import net.elytrium.limboapi.api.protocol.packets.data.MapData;
import net.elytrium.limboapi.protocol.packet.ChangeGameState;
import net.elytrium.limboapi.protocol.packet.MapDataPacket;
import net.elytrium.limboapi.protocol.packet.PlayerAbilities;
import net.elytrium.limboapi.protocol.packet.PlayerPositionAndLook;
import net.elytrium.limboapi.protocol.packet.SetSlot;
import net.elytrium.limboapi.server.world.SimpleItem;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

public class LimboPlayerImpl implements LimboPlayer {

  private final LimboAPI plugin;
  private final LimboImpl server;
  private final ConnectedPlayer player;
  private final MinecraftConnection connection;
  private final ProtocolVersion version;

  public LimboPlayerImpl(LimboAPI plugin, LimboImpl server, ConnectedPlayer player) {
    this.plugin = plugin;
    this.server = server;
    this.player = player;

    this.connection = this.player.getConnection();
    this.version = this.player.getProtocolVersion();
  }

  @Override
  public void writePacket(Object packetObj) {
    this.connection.delayedWrite(packetObj);
  }

  @Override
  public void writePacketAndFlush(Object packetObj) {
    this.connection.write(packetObj);
  }

  @Override
  public void flushPackets() {
    this.connection.flush();
  }

  @Override
  public void closeWith(Object packetObj) {
    this.connection.closeWith(packetObj);
  }

  @Override
  public void sendImage(BufferedImage image) {
    this.sendImage(0, image, true, true);
  }

  @Override
  public void sendImage(BufferedImage image, boolean sendItem) {
    this.sendImage(0, image, sendItem, true);
  }

  @Override
  public void sendImage(int mapId, BufferedImage image) {
    this.sendImage(mapId, image, true, true);
  }

  @Override
  public void sendImage(int mapId, BufferedImage image, boolean sendItem) {
    this.sendImage(mapId, image, sendItem, true);
  }

  @Override
  public void sendImage(int mapId, BufferedImage image, boolean sendItem, boolean resize) {
    if (sendItem) {
      this.setInventory(
          36,
          SimpleItem.fromItem(Item.FILLED_MAP),
          1,
          mapId,
          this.version.compareTo(ProtocolVersion.MINECRAFT_1_17) < 0
              ? null
              : CompoundBinaryTag.builder().put("map", IntBinaryTag.of(mapId)).build()
      );
    }

    if (image.getWidth() != MapData.MAP_DIM_SIZE && image.getHeight() != MapData.MAP_DIM_SIZE) {
      if (resize) {
        BufferedImage resizedImage = new BufferedImage(MapData.MAP_DIM_SIZE, MapData.MAP_DIM_SIZE, image.getType());

        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(image.getScaledInstance(MapData.MAP_DIM_SIZE, MapData.MAP_DIM_SIZE, Image.SCALE_SMOOTH), 0, 0, null);
        graphics2D.dispose();

        image = resizedImage;
      } else {
        throw new IllegalArgumentException(
            "You either need to provide an image of "
                + MapData.MAP_DIM_SIZE + "x" + MapData.MAP_DIM_SIZE
                + " pixels or set the resize parameter to true so that API will automatically resize your image."
        );
      }
    }

    int[] toWrite = MapPalette.imageToBytes(image, this.version);
    if (this.version.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
      byte[][] canvas = new byte[MapData.MAP_DIM_SIZE][MapData.MAP_DIM_SIZE];
      for (int i = 0; i < MapData.MAP_SIZE; ++i) {
        canvas[i & 127][i >> 7] = (byte) toWrite[i];
      }

      for (int i = 0; i < MapData.MAP_DIM_SIZE; ++i) {
        this.writePacket(new MapDataPacket(mapId, (byte) 0, new MapData(i, canvas[i])));
      }
      this.flushPackets();
    } else {
      byte[] canvas = new byte[MapData.MAP_SIZE];
      for (int i = 0; i < MapData.MAP_SIZE; ++i) {
        canvas[i] = (byte) toWrite[i];
      }

      this.writePacketAndFlush(new MapDataPacket(mapId, (byte) 0, new MapData(canvas)));
    }
  }

  @Override
  public void setInventory(VirtualItem item, int count) {
    this.writePacketAndFlush(new SetSlot(0, 36, item, count, 0, null));
  }

  @Override
  public void setInventory(VirtualItem item, int slot, int count) {
    this.writePacketAndFlush(new SetSlot(0, slot, item, count, 0, null));
  }

  @Override
  public void setInventory(int slot, VirtualItem item, int count, int data, CompoundBinaryTag nbt) {
    this.writePacketAndFlush(new SetSlot(0, slot, item, count, data, nbt));
  }

  @Override
  public void setGameMode(GameMode gameMode) {
    if (gameMode == GameMode.SPECTATOR && this.version.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
      return; // Spectator game mode was added in 1.8.
    }

    this.writePacketAndFlush(new ChangeGameState(3, gameMode.getId()));
  }

  @Override
  public void teleport(double x, double y, double z, float yaw, float pitch) {
    this.writePacketAndFlush(new PlayerPositionAndLook(x, y, z, yaw, pitch, 44, false, true));
  }

  /**
   * @deprecated Use {@link Player#showTitle(Title)}
   */
  @Override
  @Deprecated
  public void sendTitle(Component title, Component subtitle, ProtocolVersion version, int fadeIn, int stay, int fadeOut) {
    {
      GenericTitlePacket packet = GenericTitlePacket.constructTitlePacket(GenericTitlePacket.ActionType.SET_TITLE, version);

      packet.setComponent(ProtocolUtils.getJsonChatSerializer(version).serialize(title));
      this.writePacketAndFlush(packet);
    }
    {
      GenericTitlePacket packet = GenericTitlePacket.constructTitlePacket(GenericTitlePacket.ActionType.SET_SUBTITLE, version);

      packet.setComponent(ProtocolUtils.getJsonChatSerializer(version).serialize(subtitle));
      this.writePacketAndFlush(packet);
    }
    {
      GenericTitlePacket packet = GenericTitlePacket.constructTitlePacket(GenericTitlePacket.ActionType.SET_TIMES, version);

      packet.setFadeIn(fadeIn);
      packet.setStay(stay);
      packet.setFadeOut(fadeOut);
      this.writePacketAndFlush(packet);
    }
  }

  @Override
  public void disableFalling() {
    this.writePacketAndFlush(new PlayerAbilities((byte) 6, 0F, 0F));
  }

  @Override
  public void disconnect() {
    LimboSessionHandlerImpl handler = (LimboSessionHandlerImpl) this.connection.getSessionHandler();
    if (handler != null) {
      handler.disconnected();

      if (this.plugin.hasLoginQueue(this.player)) {
        this.plugin.getLoginQueue(this.player).next();
        return;
      }

      RegisteredServer server = handler.getPreviousServer();
      if (server != null) {
        this.sendToRegisteredServer(server);
      }
    }
  }

  @Override
  public void disconnect(RegisteredServer server) {
    LimboSessionHandlerImpl handler = (LimboSessionHandlerImpl) this.connection.getSessionHandler();
    if (handler != null) {
      handler.disconnected();

      if (this.plugin.hasLoginQueue(this.player)) {
        this.plugin.setNextServer(this.player, server);
      } else {
        this.sendToRegisteredServer(server);
      }
    }
  }

  private void sendToRegisteredServer(RegisteredServer server) {
    this.setConnectionStateRegistryToPlay();
    this.connection.eventLoop().execute(this.player.createConnectionRequest(server)::fireAndForget);
  }

  private void setConnectionStateRegistryToPlay() {
    this.connection.eventLoop().execute(() -> this.connection.setState(StateRegistry.PLAY));
  }

  @Override
  public Limbo getServer() {
    return this.server;
  }

  @Override
  public Player getProxyPlayer() {
    return this.player;
  }

  @Override
  public long getPing() {
    LimboSessionHandlerImpl handler = (LimboSessionHandlerImpl) this.connection.getSessionHandler();
    if (handler != null) {
      return handler.getPing();
    }

    return 0;
  }

  @Override
  public void disconnectFromLimboSessionHandler() {
    LimboSessionHandlerImpl handler = (LimboSessionHandlerImpl) this.connection.getSessionHandler();
    if (handler != null) {
      handler.disconnected();


      if (this.plugin.hasLoginQueue(this.player)) {
        this.plugin.getLoginQueue(this.player).next();
        return;
      }

      this.setConnectionStateRegistryToPlay();
    }
  }

  @Override
  public RegisteredServer getPreviousServer() {
    LimboSessionHandlerImpl handler = (LimboSessionHandlerImpl) this.connection.getSessionHandler();
    return handler == null ? null : handler.getPreviousServer();
  }
}
