/*
 * Copyright (C) 2021 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api;

import com.velocitypowered.api.proxy.Player;
import java.util.function.Supplier;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.VirtualChunk;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.material.Block;
import net.elytrium.limboapi.api.material.Item;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limboapi.api.protocol.PacketDirection;
import net.elytrium.limboapi.api.protocol.PreparedPacket;
import net.elytrium.limboapi.api.protocol.packets.BuiltInPackets;
import net.elytrium.limboapi.api.protocol.packets.PacketMapping;

@SuppressWarnings("unused")
public interface LimboFactory {

  /**
   * Creates new virtual block from Block enum.
   *
   * @param block Block from Block enum.
   * @return new virtual block.
   */
  VirtualBlock createSimpleBlock(Block block);

  /**
   * Creates new virtual block from id and data.
   *
   * @param legacyId Legacy block id (1.12.2 and lower)
   * @param data     Block data.
   * @return new virtual block.
   */
  VirtualBlock createSimpleBlock(short legacyId, byte data);

  /**
   * Creates new virtual customizable block.
   *
   * @return new virtual server.
   */
  VirtualBlock createSimpleBlock(boolean solid, boolean air, boolean motionBlocking, VirtualBlock.BlockInfo... blockInfos);

  /**
   * Creates new virtual world.
   *
   * @param dimension World dimension.
   * @param x         Spawn location. (X)
   * @param y         Spawn location. (Y)
   * @param z         Spawn location. (Z)
   * @param yaw       Spawn rotation. (Yaw)
   * @param pitch     Spawn rotation. (Pitch)
   * @return new virtual world.
   */
  VirtualWorld createVirtualWorld(Dimension dimension, double x, double y, double z, float yaw, float pitch);

  /**
   * Creates new virtual chunk.
   * You need to provide the chunk location, you can get it using (block_coordinate >> 4)
   *
   * @param x Chunk location. (X)
   * @param z Chunk location. (Z)
   * @return new virtual chunk.
   */
  VirtualChunk createVirtualChunk(int x, int z);

  /**
   * Creates new virtual server.
   *
   * @param world Virtual world.
   * @return new virtual server.
   */
  Limbo createLimbo(VirtualWorld world);

  /**
   * Creates new virtual server.
   *
   * @param world Virtual world.
   * @param brandName The brand name of the Limbo server to show on the Minecraft Client
   * @return new virtual server.
   */
  Limbo createLimbo(VirtualWorld world, String brandName);

  /**
   * Creates new prepared packet builder.
   *
   * @return new prepared packet.
   */
  PreparedPacket createPreparedPacket();

  /**
   * Instantiates new MinecraftPacket object.
   *
   * @param data You can find data arguments at the constructors
   *     <a href=" https://github.com/Elytrium/LimboAPI/blob/master/plugin/src/main/java/net/elytrium/limboapi/protocol/packet/">here</a>
   * @return MinecraftPacket object.
   */
  Object instantiatePacket(BuiltInPackets packetType, Object... data);

  /**
   * Registers self-made packet.
   *
   * @param direction      Packet direction.
   * @param packetClass    Packet class.
   * @param packetSupplier Packet supplier to make a new instance. (::new)
   * @param packetMappings Packet id mappings.
   */
  void registerPacket(PacketDirection direction, Class<?> packetClass, Supplier<?> packetSupplier, PacketMapping[] packetMappings);

  /**
   * Pass the player to the next Login Limbo, without spawning at current Limbo.
   *
   * @param player Player to pass.
   */
  void passLoginLimbo(Player player);

  /**
   * Creates new virtual item from Item enum.
   *
   * @param item Item from item enum.
   * @return new virtual item.
   */
  VirtualItem getItem(Item item);
}
